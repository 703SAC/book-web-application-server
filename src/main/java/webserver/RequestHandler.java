package webserver;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.invoke.LambdaConversionException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import db.DataBase;
import util.HttpRequestUtils;
import util.IOUtils;

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
        	
    		BufferedReader bufferReader =  new BufferedReader(new InputStreamReader(in));
    		DataOutputStream dos = new DataOutputStream(out);
        	String line = bufferReader.readLine();
        	if (line == null)
        		return;
        	String method = line.split(" ")[0];
        	String url = line.split(" ")[1];
        	
        	
        	
        	String requestHeader = line;
        	String rawContentType = "";
        	String contentType = "text/html";
        	String rawCookie = "";
        	boolean isLogined = false;
        	Map<String, String> cookies;
        	int contentLength = 0;
        	
        	while (!(line = bufferReader.readLine()).equals("")) {
//        		log.debug(line);
        		requestHeader += "\n" + line;
        		if (line.startsWith("Content-Length:")) {
        			String[] parts = line.split(":");
        			contentLength = Integer.parseInt(parts[1].trim());
        		}
        		if(line.startsWith("Accpet:")) {
        			String[] parts = line.split(":");
        			rawContentType = parts[1].trim();
        		}
        		if(line.startsWith("Cookie:")) {
        			String[] parts = line.split(":");
        			rawCookie = parts[1].trim();
        			cookies = HttpRequestUtils.parseCookies(rawCookie);
        			isLogined = Boolean.parseBoolean(cookies.get("logined"));
        		}
        	}
        	
        	log.debug("Request Header : " + requestHeader);
        	if(rawContentType.contains("text/css")) {
        		contentType = "text/css";
        	}
        	
    		if(method.contains("GET")) {
    			if(url.contains("/user/create?")) {
    	    		int index = url.indexOf("?");
    	    		String requestPath = url.substring(0, index);
    	    		String params = url.substring(index+1);
    	    		Map<String, String> createUserInput = HttpRequestUtils.parseQueryString(params);
    	    		
    	    		model.User newUser = new model.User(createUserInput.get("userId"), createUserInput.get("password"), createUserInput.get("name"), createUserInput.get("email"));
    	    		log.debug(newUser.getName() + newUser.getPassword() + newUser.getUserId() + newUser.getEmail());
    	    		DataBase.addUser(newUser);
    			}
    			
    			if(url.contains("/user/list") && !isLogined) {
    				response302Header(dos, "/login.html", contentType);
    			}
    			else if(url.contains("/user/list") && isLogined) {
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
			        response200Header(dos, responseBody.toString().length(), contentType);
			        responseBody(dos, responseBody.toString().getBytes());
			        return;
    			}
    			byte[] body = makeBody(url);
	    		response200Header(dos, body.length, contentType);
            	responseBody(dos, body);
        	}
    		else if(method.contains("POST")) {
    			if(url.contains("/user/create")){
    				String requestBody = IOUtils.readData(bufferReader, contentLength);
            		
            		log.debug("Request Body: " + requestBody);
        			Map<String, String> createUserInput = HttpRequestUtils.parseQueryString(requestBody);
            		
        			model.User newUser = new model.User(createUserInput.get("userId"), createUserInput.get("password"), createUserInput.get("name"), createUserInput.get("email"));
    	    		log.debug(newUser.getName() + newUser.getPassword() + newUser.getUserId() + newUser.getEmail());
    	    		DataBase.addUser(newUser);
    	    		
    				response302Header(dos, "/index.html", contentType);
    			}
    			else if(url.contains("/user/login")) {
    				String requestBody = IOUtils.readData(bufferReader, contentLength);
            		
            		log.debug("Request Body: " + requestBody);
        			Map<String, String> loginUserInput = HttpRequestUtils.parseQueryString(requestBody);
        			String loginUserId = loginUserInput.get("userId");
        			if(!DataBase.existUserId(loginUserId)) {
        				log.debug("Error : No Id");
        				response200Header(dos, "/user/login_failed.html", contentType, false);
        				return;
        			}
        			String loginUserPassword = DataBase.findUserById(loginUserId).getPassword();
        			if(loginUserInput.get("password").equals(loginUserPassword)) {
        				log.debug("Login Success");
        				response200Header(dos, "/index.html", contentType, true);
        				return;
        			}
        			
    			}
    		}
            
            
        } catch (IOException e) {
            log.error(e.getMessage());
        } catch (NullPointerException e) {
        	log.error(e.getMessage());
        }
    }
    
    private byte[] makeBody(String requestedUrl) throws IOException, NullPointerException {
    	log.debug("Requested Url : " + requestedUrl);
    	if(requestedUrl.equals("/") || requestedUrl.equals(""))
    		return "Hello World".getBytes();
    	byte[] body = Files.readAllBytes(Path.of("./webapp" + requestedUrl));
    	
    	return body;
    }
    
    private void response302Header(DataOutputStream dos, String redirectionPage, String contentType) {
        try {
        	String responseHeader = createRedirectResponseHeader(redirectionPage, contentType);
        	dos.writeBytes(responseHeader);
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
    private String createRedirectResponseHeader(String location, String contentType) {
    	StringBuilder response = new StringBuilder();
        response.append("HTTP/1.1 302 Found\r\n"); // 상태 코드 302
        response.append("Location: ").append(location).append("\r\n"); // 리다이렉트할 URL
        response.append("Content-Type: " + contentType + "\r\n"); // 콘텐츠 타입
        response.append("Content-Length: 0\r\n"); // 본문 길이 (없으므로 0)
        response.append("\r\n"); // 헤더와 본문 구분.
        return response.toString();
    }
    
    private String create200ResponseHeader(int lengthOfBodyContent, String contentType) {
    	StringBuilder response = new StringBuilder();
        response.append("HTTP/1.1 200 OK\r\n"); // 상태 코드 302
        response.append("Content-Type: " + contentType + ";charset=utf-8\r\n"); // 콘텐츠 타입
        response.append("Content-Length: " + lengthOfBodyContent + "\r\n"); // 본문 길이 (없으므로 0)
        response.append("\r\n"); // 헤더와 본문 구분.
        return response.toString();
    }
    private String create200SetCookieResponseHeader(String location, String contentType, boolean isLoginSuccessed) {
    	StringBuilder response = new StringBuilder();
//    	if(isLoginSuccessed) {
//    		response.append("HTTP/1.1 200 OK\r\n"); // 상태 코드 302
//    	}
//    	else {
//    		response.append("HTTP/1.1 302 Found\r\n");
//    	}
    	response.append("HTTP/1.1 302 Found\r\n");
        response.append("Location: ").append(location).append("\r\n"); // 리다이렉트할 URL
        response.append("Content-Type: " + contentType + ";charset=utf-8\r\n"); // 콘텐츠 타입
        response.append("Content-Length: " + "0" + "\r\n"); // 본문 길이 (없으므로 0)
        response.append("Set-Cookie: logined=" + isLoginSuccessed);
        response.append("\r\n"); // 헤더와 본문 구분.
        return response.toString();
    }
    
    private void response200Header(DataOutputStream dos, String location, String contentType, boolean isLoginSuccessed) {
        try {
        	String responseHeader = create200SetCookieResponseHeader(location, contentType, isLoginSuccessed);
        	dos.writeBytes(responseHeader);
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
    
    private void response200Header(DataOutputStream dos, int lengthOfBodyContent, String contentType) {
        try {
        	String responseHeader = create200ResponseHeader(lengthOfBodyContent, contentType);
        	dos.writeBytes(responseHeader);
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
