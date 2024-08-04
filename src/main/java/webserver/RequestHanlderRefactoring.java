package webserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Collection;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controller.Controller;
import db.DataBase;
import util.HttpRequestUtils;

public class RequestHanlderRefactoring extends Thread {
	private static final Logger log = LoggerFactory.getLogger(RequestHandler.class);

    private Socket connection;

    public RequestHanlderRefactoring(Socket connectionSocket) {
        this.connection = connectionSocket;
    }
    
    public void run() {
        log.debug("New Client Connect! Connected IP : {}, Port : {}", connection.getInetAddress(),
                connection.getPort());

        try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream())); 
        		PrintWriter out = new PrintWriter(connection.getOutputStream(), true)) {
            // TODO 사용자 요청에 대한 처리는 이 곳에 구현하면 된다.
            Request request = new Request(in);
            Response response = new Response(out);
            Controller controller = RequestMapping.getController(request.getUrl());
            if (controller == null) {
            	String path = getDefaultPath(request.getUrl());
            	response.sendFile(path);
            }
            else {
            	controller.service(request, response);
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        } catch (NullPointerException e) {
        	log.error(e.getMessage());
        	return;
        }
    }
    
    private String getDefaultPath(String path) {
		if(path.equals("/"))
			return "/index.html";
		return path;
	}
    
}
