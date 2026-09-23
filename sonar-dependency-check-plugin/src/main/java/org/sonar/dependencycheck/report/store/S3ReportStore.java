/*
 * Dependency-Check Plugin for SonarQube
 * Copyright (C) 2015-2025 dependency-check
 * philipp.dallig@gmail.com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.dependencycheck.report.store;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.config.Configuration;
import org.sonar.dependencycheck.base.DependencyCheckConstants;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;

/**
 * Keeps reports in an S3 bucket. Works against Amazon S3 and, with an endpoint and path style
 * access, against S3 compatible servers such as MinIO.
 */
public class S3ReportStore implements ReportStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(S3ReportStore.class);
    private static final String CONTENT_TYPE = "text/html";

    private final S3Client client;
    private final String bucket;
    private final String prefix;

    public S3ReportStore(S3Client client, String bucket, String prefix) {
        this.client = client;
        this.bucket = bucket;
        this.prefix = normalisePrefix(prefix);
    }

    @Override
    public String store(String key, InputStream report) throws ReportStoreException {
        String objectKey = objectKey(key);
        Path spooled = null;
        try {
            // The SDK needs a known content length, and the stream does not carry one. Spooling to
            // a temporary file keeps the report off the heap - it can be tens of megabytes.
            spooled = Files.createTempFile("dependency-check-", ".html");
            // Written through an OutputStream, not with Files.copy(REPLACE_EXISTING): that deletes
            // the 0600 file createTempFile just made and re-creates it with the process umask,
            // which would leave a private project's vulnerability report readable by every local
            // account for as long as the upload runs.
            try (OutputStream out = Files.newOutputStream(spooled, StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE)) {
                copy(report, out);
            }
            client.putObject(PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectKey)
                    .contentType(CONTENT_TYPE)
                    .build(), RequestBody.fromFile(spooled));
        } catch (SdkException | IOException e) {
            throw new ReportStoreException("Could not write the report to s3://" + bucket + "/" + objectKey, e);
        } finally {
            if (spooled != null) {
                try {
                    Files.deleteIfExists(spooled);
                } catch (IOException e) {
                    LOGGER.debug("Could not delete the temporary report file {}", spooled, e);
                }
            }
        }
        return "s3://" + bucket + "/" + objectKey;
    }

    /** Copies {@code in} to {@code out} without holding the report in memory. */
    private static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[8192];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }

    @Override
    public Optional<InputStream> read(String key) throws ReportStoreException {
        String objectKey = objectKey(key);
        try {
            ResponseInputStream<?> object = client.getObject(GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectKey)
                    .build());
            return Optional.of(object);
        } catch (NoSuchKeyException e) {
            return Optional.empty();
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return Optional.empty();
            }
            throw new ReportStoreException("Could not read the report from s3://" + bucket + "/" + objectKey, e);
        } catch (SdkException e) {
            throw new ReportStoreException("Could not read the report from s3://" + bucket + "/" + objectKey, e);
        }
    }

    @Override
    public boolean exists(String key) throws ReportStoreException {
        String objectKey = objectKey(key);
        try {
            client.headObject(HeadObjectRequest.builder().bucket(bucket).key(objectKey).build());
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return false;
            }
            throw new ReportStoreException("Could not look up the report at s3://" + bucket + "/" + objectKey, e);
        } catch (SdkException e) {
            throw new ReportStoreException("Could not look up the report at s3://" + bucket + "/" + objectKey, e);
        }
    }

    @Override
    public List<String> list(String prefix) throws ReportStoreException {
        String objectPrefix = objectKey(prefix);
        List<String> keys = new ArrayList<>();
        try {
            String continuationToken = null;
            do {
                ListObjectsV2Request.Builder requestBuilder = ListObjectsV2Request.builder()
                        .bucket(bucket)
                        .prefix(objectPrefix);
                if (continuationToken != null) {
                    requestBuilder.continuationToken(continuationToken);
                }
                ListObjectsV2Response response = client.listObjectsV2(requestBuilder.build());
                for (S3Object object : response.contents()) {
                    keys.add(storeKey(object.key()));
                }
                continuationToken = Boolean.TRUE.equals(response.isTruncated()) ? response.nextContinuationToken() : null;
            } while (continuationToken != null);
        } catch (SdkException e) {
            throw new ReportStoreException("Could not list the reports under s3://" + bucket + "/" + objectPrefix, e);
        }
        return keys;
    }

    @Override
    public boolean delete(String key) throws ReportStoreException {
        if (key.trim().isEmpty()) {
            // An empty key is the store's own prefix, not a report.
            throw new ReportStoreException("Refusing to delete the report store itself");
        }
        String objectKey = objectKey(key);
        try {
            client.headObject(HeadObjectRequest.builder().bucket(bucket).key(objectKey).build());
        } catch (NoSuchKeyException e) {
            return false;
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return false;
            }
            throw new ReportStoreException("Could not delete the report at s3://" + bucket + "/" + objectKey, e);
        } catch (SdkException e) {
            throw new ReportStoreException("Could not delete the report at s3://" + bucket + "/" + objectKey, e);
        }
        try {
            client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(objectKey).build());
        } catch (SdkException e) {
            throw new ReportStoreException("Could not delete the report at s3://" + bucket + "/" + objectKey, e);
        }
        return true;
    }

    @Override
    public void close() {
        client.close();
    }

    private String objectKey(String key) {
        return prefix.isEmpty() ? key : prefix + "/" + key;
    }

    /** The inverse of {@link #objectKey(String)}: strips the store's own configured prefix. */
    private String storeKey(String objectKey) {
        if (prefix.isEmpty()) {
            return objectKey;
        }
        String prefixWithSlash = prefix + "/";
        return objectKey.startsWith(prefixWithSlash) ? objectKey.substring(prefixWithSlash.length()) : objectKey;
    }

    private static String normalisePrefix(String prefix) {
        String trimmed = prefix == null ? "" : prefix.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        while (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1);
        }
        return trimmed;
    }

    public static S3ReportStore from(Configuration config) throws ReportStoreException {
        // A present but blank bucket is not a configured bucket - every request would fail with an
        // SDK error naming nothing the administrator could act on.
        String bucket = config.get(DependencyCheckConstants.HTML_REPORT_S3_BUCKET_PROPERTY)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .orElseThrow(() -> new ReportStoreException("Report store 's3' needs a bucket. Please check property "
                        + DependencyCheckConstants.HTML_REPORT_S3_BUCKET_PROPERTY));
        String prefix = config.get(DependencyCheckConstants.HTML_REPORT_S3_PREFIX_PROPERTY)
                .orElse(DependencyCheckConstants.HTML_REPORT_S3_PREFIX_DEFAULT);
        String region = config.get(DependencyCheckConstants.HTML_REPORT_S3_REGION_PROPERTY)
                .orElse(DependencyCheckConstants.HTML_REPORT_S3_REGION_DEFAULT);
        boolean pathStyleAccess = config.getBoolean(DependencyCheckConstants.HTML_REPORT_S3_PATH_STYLE_ACCESS_PROPERTY)
                .orElse(DependencyCheckConstants.HTML_REPORT_S3_PATH_STYLE_ACCESS_DEFAULT);

        S3ClientBuilder builder = S3Client.builder()
                .httpClientBuilder(UrlConnectionHttpClient.builder())
                .region(parseRegion(region))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(pathStyleAccess).build());

        Optional<String> endpoint = config.get(DependencyCheckConstants.HTML_REPORT_S3_ENDPOINT_PROPERTY);
        if (endpoint.isPresent() && !endpoint.get().trim().isEmpty()) {
            builder = builder.endpointOverride(parseEndpoint(endpoint.get().trim()));
        }

        // Without explicit keys the SDK default chain applies, so IAM roles work without any
        // configuration at all.
        Optional<String> accessKeyId = nonBlank(config, DependencyCheckConstants.HTML_REPORT_S3_ACCESS_KEY_ID_PROPERTY);
        Optional<String> secretAccessKey = nonBlank(config,
                DependencyCheckConstants.HTML_REPORT_S3_SECRET_ACCESS_KEY_PROPERTY);
        if (accessKeyId.isPresent() || secretAccessKey.isPresent()) {
            // Half a key pair is a configuration mistake, not a reason to fall back to the default
            // chain silently - and the SDK would reject a blank value with an unchecked exception
            // out here, where no handler turns it into a message naming the property to fix.
            if (!accessKeyId.isPresent() || !secretAccessKey.isPresent()) {
                throw new ReportStoreException("Report store 's3' needs both an access key id and a secret access key, "
                        + "or neither. Please check the properties "
                        + DependencyCheckConstants.HTML_REPORT_S3_ACCESS_KEY_ID_PROPERTY + " and "
                        + DependencyCheckConstants.HTML_REPORT_S3_SECRET_ACCESS_KEY_PROPERTY);
            }
            Optional<String> sessionToken = nonBlank(config,
                    DependencyCheckConstants.HTML_REPORT_S3_SESSION_TOKEN_PROPERTY);
            if (sessionToken.isPresent()) {
                builder = builder.credentialsProvider(StaticCredentialsProvider.create(
                        AwsSessionCredentials.create(accessKeyId.get(), secretAccessKey.get(), sessionToken.get())));
            } else {
                builder = builder.credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKeyId.get(), secretAccessKey.get())));
            }
        }

        try {
            return new S3ReportStore(builder.build(), bucket, prefix);
        } catch (SdkException | IllegalArgumentException e) {
            // The SDK validates region and endpoint while building, and rejects a malformed value
            // with an unchecked exception. Unchecked, it would leave the web service handlers -
            // which only catch ReportStoreException and IOException - and reach the user as a 500
            // with a stack trace instead of a message naming the setting to fix.
            throw new ReportStoreException("Could not create the S3 client. Please check the properties "
                    + DependencyCheckConstants.HTML_REPORT_S3_REGION_PROPERTY + " and "
                    + DependencyCheckConstants.HTML_REPORT_S3_ENDPOINT_PROPERTY, e);
        }
    }

    /** The trimmed value of {@code property}, or an empty optional if it is unset or blank. */
    private static Optional<String> nonBlank(Configuration config, String property) {
        return config.get(property).map(String::trim).filter(value -> !value.isEmpty());
    }

    /** {@code Region.of} rejects a null or empty value with an IllegalArgumentException. */
    private static Region parseRegion(String region) throws ReportStoreException {
        try {
            return Region.of(region);
        } catch (IllegalArgumentException e) {
            throw new ReportStoreException("'" + region + "' is not a valid AWS region. Please check property "
                    + DependencyCheckConstants.HTML_REPORT_S3_REGION_PROPERTY, e);
        }
    }

    /** Same for {@code URI.create}, which throws on a malformed endpoint. */
    private static URI parseEndpoint(String endpoint) throws ReportStoreException {
        try {
            return URI.create(endpoint);
        } catch (IllegalArgumentException e) {
            throw new ReportStoreException("'" + endpoint + "' is not a valid endpoint URI. Please check property "
                    + DependencyCheckConstants.HTML_REPORT_S3_ENDPOINT_PROPERTY, e);
        }
    }
}
