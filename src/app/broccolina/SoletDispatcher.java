package app.broccolina;

import app.broccolina.solet.*;
import app.broccolina.util.ApplicationLoadingService;
import app.broccolina.util.SessionManagementService;
import app.javache.api.RequestHandler;
import app.javache.io.Reader;
import app.javache.io.Writer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SoletDispatcher implements RequestHandler {

    private final String serverRootFolderPath;

    private boolean hasIntercepted;

    private Map<String, HttpSolet> soletMap;

    private ApplicationLoadingService applicationLoadingService;

    private SessionManagementService sessionManagementService;

    public SoletDispatcher(String serverRootFolderPath) {
        this.serverRootFolderPath = serverRootFolderPath;
        this.hasIntercepted = false;
        this.applicationLoadingService = new ApplicationLoadingService();
        this.sessionManagementService = new SessionManagementService();
        this.initializeSoletMap();
    }

    private void initializeSoletMap() {
        try {
            this.soletMap = this.applicationLoadingService.loadApplications(serverRootFolderPath + "webapps" + File.separator);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream) {
        try {
            HttpSoletRequest request = new HttpSoletRequestImpl(new Reader().readAllLines(inputStream), inputStream);
            HttpSoletResponse response = new HttpSoletResponseImpl(outputStream);

            this.sessionManagementService.initSessionIfExistent(request);

            HttpSolet soletObject = this.findSoletCandidate(request, response);

            if (request.isResource() || soletObject == null) {
                this.hasIntercepted = false;
                return;
            }

            Class[] soletServiceMethodParameters = Arrays
                    .stream(soletObject.getClass().getMethods())
                    .filter(m -> m.getName().equals("service"))
                    .findFirst()
                    .orElse(null)
                    .getParameterTypes();

            soletObject.getClass()
                    .getMethod("service", soletServiceMethodParameters[0], soletServiceMethodParameters[1])
                    .invoke(soletObject, request, response);

            this.sessionManagementService.sendSessionIfExistent(request, response);

            this.sessionManagementService.clearInvalidSessions();
//            response.setStatusCode(HttpStatus.OK);
//            response.addHeader("Content-Type", "text/html");
//            response.setContent("Hi from broko".getBytes());
            new Writer().writeBytes(response.getBytes(), response.getOutputStream());

            this.hasIntercepted = true;
        } catch (IOException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            e.printStackTrace();
            this.hasIntercepted = false;
        }
    }

    private HttpSolet findSoletCandidate(HttpSoletRequest request, HttpSoletResponse response) {
        String requestUrl = request.getRequestUrl();

        Pattern applicationRouteMatchPattern = Pattern.compile("\\/[a-zA-Z0-9]+\\/");
        Matcher applicationRouteMatcher = applicationRouteMatchPattern.matcher(requestUrl);

        if (this.soletMap.containsKey(requestUrl)) {
            return this.soletMap.get(requestUrl);
        }

        if (applicationRouteMatcher.find()) {
            String applicationRoute = applicationRouteMatcher.group(0) + "*";

            if (this.soletMap.containsKey(applicationRoute)) {
                return this.soletMap.get(applicationRoute);
            }
        }

        if (this.soletMap.containsKey("/*")) {
            return this.soletMap.get("/*");
        }

        return null;
    }

    @Override
    public boolean hasIntercepted() {
        return this.hasIntercepted;
    }
}
