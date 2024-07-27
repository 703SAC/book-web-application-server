package webserver;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;

public class Response {
	private PrintWriter out;
	
	public Response(PrintWriter out) {
		this.out = out;
	}
	
	public void send(String message, int statusCode) {
		out.println("HTTP/1.1 " + statusCode + " OK");
        out.println("Content-Type: text/html; charset=UTF-8");
        out.println("Content-Length: " + message.length());
        out.println(); // 빈 줄
        out.println(message);
        out.flush();
	}
	public void sendCss(String cssContent) {
		out.println("HTTP/1.1 200 OK");
        out.println("Content-Type: text/css; charset=UTF-8");
        out.println("Content-Length: " + cssContent.length());
        out.println(); // 빈 줄
        out.println(cssContent);
        out.flush();
	}
	public void sendNotFound() {
        String message = "404 Not Found";
        out.println("HTTP/1.1 404 Not Found");
        out.println("Content-Type: text/plain; charset=UTF-8");
        out.println("Content-Length: " + message.length());
        out.println(); // 빈 줄
        out.println(message);
        out.flush();
    }
	public void sendRedirection(String location) {
    	out.println("HTTP/1.1 302 Found"); // 상태 코드 302
    	out.println("Location: " + location); // 리다이렉트할 URL
    	out.println("Content-Type: text/html; charset=UTF-8" ); // 콘텐츠 타입
        out.println("Content-Length: 0"); // 본문 길이 (없으므로 0)
        out.println(); // 헤더와 본문 구분.
        out.flush();
    }
	public void sendLoginSuccessed(String location) {
    	out.println("HTTP/1.1 302 Found"); // 상태 코드 302
    	out.println("Location: " + location); // 리다이렉트할 URL
    	out.println("Content-Type: text/html; charset=UTF-8" ); // 콘텐츠 타입
    	out.println("Set-Cookie: logined=true");
        out.println("Content-Length: 0"); // 본문 길이 (없으므로 0)
        out.println(); // 헤더와 본문 구분.
        out.flush();
    }
	public void sendFile(String filename) throws IOException {
		String filePath = "./webapp" + filename;
		File file = new File(filePath);
		if (file.exists() && !file.isDirectory()) {
			String fileContent = Files.readString(Path.of(filePath));
			String mimeType = getMimeType(filename);
            long contentLength = fileContent.length();
            out.println("HTTP/1.1 200 OK");
            out.println("Content-Type: " + mimeType + "; charset=UTF-8");
            out.println("Content-Length: " + contentLength);
            out.println(); // 빈 줄
            out.println(fileContent);
            out.flush();
	    } else {
	        sendNotFound();
	    }
	}
	
	private String getMimeType(String filename) {
	    if (filename.endsWith(".css")) {
	        return "text/css";
	    } else if (filename.endsWith(".js")) {
	        return "application/javascript";
	    } else if (filename.endsWith(".html")) {
	        return "text/html";
	    } else if (filename.endsWith(".png")) {
	        return "image/png";
	    } else if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) {
	        return "image/jpeg";
	    }
	    return "application/octet-stream"; // 기본 MIME 타입
	}
}
