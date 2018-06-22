package app.broccolina.solet;

import app.javache.http.HttpResponse;

import java.io.OutputStream;

public interface HttpSoletResponse extends HttpResponse {

    OutputStream getOutputStream();
}
