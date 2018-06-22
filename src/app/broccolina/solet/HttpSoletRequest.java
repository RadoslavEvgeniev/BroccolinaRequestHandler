package app.broccolina.solet;

import app.javache.http.HttpRequest;

import java.io.InputStream;

public interface HttpSoletRequest extends HttpRequest {

    InputStream getInputStream();
}
