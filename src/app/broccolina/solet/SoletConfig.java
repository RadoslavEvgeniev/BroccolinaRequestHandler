package app.broccolina.solet;

import java.util.HashMap;

public interface SoletConfig {

    HashMap<String, Object> getAttributes();

    void setAttribute(String name, Object attribute);

    Object getAttribute(String name);

    void deleteAttribute(String name);
}
