package org.graalvm.argo.graalvisor.base;

import org.graalvm.polyglot.Engine;

public class TruffleFunction extends PolyglotFunction {

    private final String source;
    private Engine engine;

	public TruffleFunction(String name, String entryPoint, String language, String source) {
		super(name, entryPoint, language);
		this.source = source;
		this.engine = Engine.create();
	}

    public Engine getTruffleEngine() {
        return engine;
    }

    public String getSource() {
        return source;
    }
}
