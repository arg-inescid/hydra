package org.graalvm.argo.graalvisor.base;

import static org.graalvm.argo.graalvisor.Proxy.APP_DIR;
import java.io.FileNotFoundException;
import com.oracle.svm.graalvisor.api.GraalVisorAPI;

public class NativeFunction extends PolyglotFunction {

	private GraalVisorAPI graalVisorAPI;

	public NativeFunction(String name, String entryPoint, String language) {
		super(name, entryPoint, language);
		try {
            this.graalVisorAPI = new GraalVisorAPI(APP_DIR + name);
        } catch (FileNotFoundException e) {
            System.err.println("SO file not found.");
            e.printStackTrace();
        }
	}

    public GraalVisorAPI getGraalVisorAPI() {
        return graalVisorAPI;
    }
}
