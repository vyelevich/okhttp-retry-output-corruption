import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static java.lang.String.format;


public class Uploader {

    private static final Logger logger = LoggerFactory.getLogger(Uploader.class);

    private static final MediaType MEDIA_TYPE = MediaType.parse("application/octet-stream");

    private final OkHttpClient okHttpClient;

    public Uploader() {
        this.okHttpClient = newOkHttpClient();
    }

    private OkHttpClient newOkHttpClient() {
        return new OkHttpClient.Builder()
                .addInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.HEADERS))
                .build();
    }

    public void upload(String url, String data) throws IOException {
        logger.info("Uploading to URL: {}", url);

        //First try to upload with a body which fails to read source stream
        try {
            logger.info("Produce error while reading body");
            uploadStream(url, createRequestBodyFailsDuringIO(data.length()));
        } catch (Throwable e) {
            logger.info("Handling upload error", e);
        }

        //Second try - should be OK
        try (InputStream inputStream = new ByteArrayInputStream(data.getBytes())) {
            logger.info("No EOF");
            long contentLength = data.length();
            uploadStream(url, createRequestBodyHappyPath(inputStream, contentLength));
        }
    }

    private void uploadStream(String url, RequestBody requestBody) throws IOException {
        Request request = new Request.Builder().url(url).put(requestBody).build();
        Response response = this.okHttpClient.newCall(request).execute();
        if (!response.isSuccessful()) {
            throw new IllegalStateException(format("Unsuccessful response: %s", response.code()));
        }
    }

    public static RequestBody createRequestBodyFailsDuringIO(long contentLength) {
        return new RequestBody() {
            @Override
            public MediaType contentType() {
                return MEDIA_TYPE;
            }

            @Override
            public long contentLength() {
                return contentLength;
            }

            @SuppressWarnings("RedundantThrows")
            @Override
            public void writeTo(BufferedSink sink) throws IOException {
                throw new RuntimeException("Simulate fails to read bytes from source");
            }
        };
    }

    public static RequestBody createRequestBodyHappyPath(InputStream inputStream, long contentLength) {
        return new RequestBody() {
            @Override
            public MediaType contentType() {
                return MEDIA_TYPE;
            }

            @Override
            public long contentLength() {
                return contentLength;
            }

            @Override
            public void writeTo(BufferedSink sink) throws IOException {
                try (Source source = Okio.source(inputStream)) {
                    sink.writeAll(source);
                }
            }
        };
    }
}
