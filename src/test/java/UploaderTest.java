import org.junit.Test;

import java.io.IOException;

import static java.lang.String.format;
import static org.junit.Assert.assertEquals;

public class UploaderTest {

    @Test
    public void uploadDataMatchesReceivedData() throws IOException {
        String host = "localhost";
        int port = 3001;
        String path = "/debug";
        String uploadUrl = format("http://%s:%s%s", host, port, path);

        String sendData = "Some data.";

        DebugHttpServer httpServer = new DebugHttpServer(host, port, path).run();
        new Uploader().upload(uploadUrl, sendData);

        String receivedData = httpServer.getLastRequestBody();
        assertEquals(sendData, receivedData);
    }
}