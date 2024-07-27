package webserver;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import db.DataBase;
import util.HttpRequestUtils;
import util.IOUtils;

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
            handleRequest(request, response);
        } catch (IOException e) {
            log.error(e.getMessage());
        } catch (NullPointerException e) {
        	log.error(e.getMessage());
        	return;
        }
    }
    
    private void handleRequest(Request request, Response response) throws IOException {
    	switch(request.getMethod()){
    	case "GET":
    		handleGetRequest(request, response);
    		break;
    	case "POST":
    		handlePostRequest(request, response);
    		break;
    	default:
    		response.sendNotFound();
    		break;
    	}
    }
    
    private void handleGetRequest(Request request, Response response) throws IOException{
    	String url = request.getUrl();
    	switch(url) {
    	case "/":
    		response.send("Hello World!", 200);
    		break;
    	case "/user/create":
    		response.send("Success", 200);
    		break;
    	case "/user/list":
    		if(request.getIsLogined()) {
    			Collection<model.User> users = DataBase.findAll();
				StringBuilder responseBody = new StringBuilder();
		        responseBody.append("<html><head><title>User List</title></head><body>");
		        responseBody.append("<h1>User List</h1>");
		        responseBody.append("<ul>");
		        
		        for (model.User user : users) {
		            responseBody.append("<li>").append(user.toString()).append("</li>");
		        }
		        responseBody.append("</ul>");
		        responseBody.append("</body></html>");
		        response.send(responseBody.toString(), 200);
    		}
    		else if(!request.getIsLogined()){
    			response.sendRedirection("/user/login.html");
    		}
    		break;
    	default:
    		response.sendFile(url);
    		break;
    		
    	
    	}
    }
    
    private void handlePostRequest(Request request, Response response) {
    	String url = request.getUrl();
    	switch(url) {
    	case "/user/create":
    		String userToCreate = request.getBody();
    		Map<String, String> createUserInput = HttpRequestUtils.parseQueryString(userToCreate);
    		model.User newUser = new model.User(createUserInput.get("userId"), createUserInput.get("password"), createUserInput.get("name"), createUserInput.get("email"));
    		DataBase.addUser(newUser);
    		response.sendRedirection("/index.html");
    		break;
    	case "/user/login":
    		String userToLogin = request.getBody();
			Map<String, String> loginUserInput = HttpRequestUtils.parseQueryString(userToLogin);
			
			String loginUserId = loginUserInput.get("userId");
			if(!DataBase.existUserId(loginUserId)) {
				response.sendRedirection("/user/login_failed.html");
			}
			else if (DataBase.existUserId(loginUserId)) {
				String loginUserPassword = DataBase.findUserById(loginUserId).getPassword();
				if(loginUserInput.get("password").equals(loginUserPassword)) {
					
					response.sendLoginSuccessed("/index.html");
				}
				else if(!loginUserInput.get("password").equals(loginUserPassword)) {
					response.sendRedirection("/user/login_failed.html");
				}
			}
    		break;
    	}
    }
    
}
