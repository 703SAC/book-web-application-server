package webserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import db.DataBase;
import util.HttpRequestUtils;

public class Request {
	private String method;
	private String url;
	private String body;
	private Map<String, String> cookies;
	private boolean isLogined;
	private Map<String, String> parameters;
	
	public Request(BufferedReader in) throws IOException, NullPointerException{
		String requestLine = in.readLine();
		if(requestLine != null) {
			String[] parts = requestLine.split(" ");
			this.method = parts[0];
			this.url = parts[1];
			parseUrl(parts[1]);
			readHeaders(in);
		}
		else {
			throw new NullPointerException();
		}
	}
	 private void parseUrl(String url) {
	        int questionMarkIndex = url.indexOf('?');
	        if (questionMarkIndex != -1) {
	            this.url = url.substring(0, questionMarkIndex);
	            String queryString = url.substring(questionMarkIndex + 1);
	            this.parameters = HttpRequestUtils.parseQueryString(queryString);
	    		
	    		model.User newUser = new model.User(parameters.get("userId"), parameters.get("password"), parameters.get("name"), parameters.get("email"));
	    		DataBase.addUser(newUser);

	        } else {
	            this.url = url;
	            this.parameters = new HashMap<>();
	        }
    }

	
	private void readHeaders(BufferedReader in) throws IOException{
		String headerLine;
		int contentLength = 0;
		while(!(headerLine = in.readLine()).isEmpty()) {
			if(headerLine.startsWith("Cookie:")) {
				this.cookies = HttpRequestUtils.parseCookies(headerLine.substring(7).trim());
    			this.isLogined = Boolean.parseBoolean(this.cookies.get("logined"));
			}
			else if(headerLine.startsWith("Content-Length:")) {
				contentLength = Integer.parseInt(headerLine.split(":")[1].trim());
                
			}
		}
		if(contentLength > 0){
			this.body = readBody(in, contentLength);
		}
		else {
			this.body = "";
		}
	}
	public boolean getIsLogined() {
		return this.isLogined;
	}
	
	private String readBody(BufferedReader in, int contentLength) throws IOException {
        StringBuilder bodyBuilder = new StringBuilder();
        char[] buffer = new char[1024];
        int totalRead = 0;

        while (totalRead < contentLength) {
            int bytesRead = in.read(buffer, 0, Math.min(buffer.length, contentLength - totalRead));
            if (bytesRead == -1) {
                break; // EOF
            }
            bodyBuilder.append(buffer, 0, bytesRead);
            totalRead += bytesRead;
        }
        return bodyBuilder.toString();
    }

    public String getMethod() {
        return method;
    }

    public String getUrl() {
        return url;
    }

    public String getBody() {
        return body;
    }

	
}
