package app.broccolina.solet;

import java.util.Map;

public interface SoletConfig {

    Map<String, Object> getAttributes();

    void setAttribute(String name, Object attribute);

    Object getAttribute(String name);

    void deleteAttribute(String name);
}
