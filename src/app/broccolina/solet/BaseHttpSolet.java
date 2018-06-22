package app.broccolina.solet;

import app.javache.http.HttpStatus;
import app.javache.io.Writer;

import java.io.IOException;

public abstract class BaseHttpSolet implements HttpSolet {

    private boolean isInitialized;

    private SoletConfig soletConfig;

    protected BaseHttpSolet() {
        this.isInitialized = false;
    }

    private void configureNotFound(HttpSoletResponse response) {
        response.setStatusCode(HttpStatus.NOT_FOUND);

        response.addHeader("Content-Type", "text/html");
    }

    @Override
    public void init(SoletConfig soletConfig) {
        this.soletConfig = soletConfig;
    }

    @Override
    public SoletConfig getSoletConfig() {
        return this.soletConfig;
    }

    @Override
    public void service(HttpSoletRequest request, HttpSoletResponse response) throws IOException {
        if (request.getMethod().equals("GET")) {
            this.doGet(request, response);
        } else if (request.getMethod().equals("POST")) {
            this.doPost(request, response);
        } else if (request.getMethod().equals("PUT")) {
            this.doPut(request, response);
        } else if (request.getMethod().equals("DELETE")) {
            this.doDelete(request, response);
        }

        Writer.writeBytes(response.getBytes(), response.getOutputStream());
    }

    protected void doGet(HttpSoletRequest request, HttpSoletResponse response) {
        this.configureNotFound(response);
        response.setContent(("[ERROR] GET " + request.getRequestUrl() + " The page or functionality you are looking for is not found.").getBytes());
    }

    protected void doPost(HttpSoletRequest request, HttpSoletResponse response) {
        this.configureNotFound(response);
        response.setContent(("[ERROR] POST " + request.getRequestUrl() + " The page or functionality you are looking for is not found.").getBytes());
    }

    protected void doPut(HttpSoletRequest request, HttpSoletResponse response) {
        this.configureNotFound(response);
        response.setContent(("[ERROR] PUT " + request.getRequestUrl() + " The page or functionality you are looking for is not found.").getBytes());
    }

    protected void doDelete(HttpSoletRequest request, HttpSoletResponse response) {
        this.configureNotFound(response);
        response.setContent(("[ERROR] DELETE " + request.getRequestUrl() + " The page or functionality you are looking for is not found.").getBytes());
    }
}
