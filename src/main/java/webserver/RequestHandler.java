package webserver;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RequestHandler extends Thread {
    private static final Logger log = LoggerFactory.getLogger(RequestHandler.class);

    private Socket connection;

    public RequestHandler(Socket connectionSocket) {
        this.connection = connectionSocket;
    }

    public void run() {
        log.debug("New Client Connect! Connected IP : {}, Port : {}", connection.getInetAddress(),
                connection.getPort());

        try (InputStream in = connection.getInputStream(); OutputStream out = connection.getOutputStream()) {
            // TODO 사용자 요청에 대한 처리는 이 곳에 구현하면 된다.
        	try {
        		String url = extractUrlFromInputStream(in);
        		DataOutputStream dos = new DataOutputStream(out);
                byte[] body = makeBody(url);
                response200Header(dos, body.length);
                responseBody(dos, body);
        	}
        	catch(NullPointerException e) {
        		log.debug("NullPointerException: " + e.getMessage() + " // " + e.getLocalizedMessage());
        		return;
        	}
            
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
    
    private byte[] makeBody(String requestedUrl) throws IOException, NullPointerException {
    	log.debug("Requested Url : " + requestedUrl);
    	if(requestedUrl.equals("/"))
    		return "Hello World".getBytes();
    	byte[] body =Files.readAllBytes(Path.of("./webapp" + requestedUrl));
    	
    	return body;
    }
    private String extractUrlFromInputStream(InputStream in) throws IOException, NullPointerException {
    	String url = "";
    	BufferedReader bufferReader =  new BufferedReader(new InputStreamReader(in));
    	String line = bufferReader.readLine();
    	if (line == null)
    		return "";
    	
    	String method = line.split(" ")[0];
    	url = line.split(" ")[1];
    	log.debug("line: " + line + " // method: " + method + " // url" + url);
    	
    	while (!(line = bufferReader.readLine()).equals("")) {
    		log.debug(line);
    	}
    	
    	return url;
    }

    private void response200Header(DataOutputStream dos, int lengthOfBodyContent) {
        try {
            dos.writeBytes("HTTP/1.1 200 OK \r\n");
            dos.writeBytes("Content-Type: text/html;charset=utf-8\r\n");
            dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void responseBody(DataOutputStream dos, byte[] body) {
        try {
            dos.write(body, 0, body.length);
            dos.flush();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
}
