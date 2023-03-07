package com.oracle.svm.graalvisor.guestapi;

import java.util.HashMap;
import java.util.Map;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;

import com.oracle.svm.graalvisor.utils.PolyglotHostAccess;

public class PolyglotEngine {

    private final Value function;

    private static final Engine engine = Engine.create();

    public PolyglotEngine(String language, String source, String entrypoint) {
        Map<String, String> options = new HashMap<>();
        String javaHome = System.getenv("JAVA_HOME");

        if (javaHome == null) {
            System.err.println("JAVA_HOME not found in the environment. Polyglot functionality significantly limited.");
        } else {
            System.setProperty("org.graalvm.language.python.home", javaHome + "/languages/python");
            System.setProperty("org.graalvm.language.llvm.home", javaHome + "/languages/llvm");
            System.setProperty("org.graalvm.language.js.home", javaHome + "/languages/js");
        }

        if (language.equals("python")) {
            // Necessary to allow python imports.
            options.put("python.ForceImportSite", "true");
            // Loading the virtual env with installed packages
            options.put("python.Executable", javaHome + "/graalvisor-python-venv/bin/python");
        }

        // Build context.
        Context context = Context.newBuilder().allowAllAccess(true).engine(engine).options(options).build();
        System.out.println(String.format("[thread %s] Creating context %s", Thread.currentThread().getId(), context.toString()));

        // Host access to implement missing language functionalities.
        addBindings(language, context);

        // Evaluate source script to load function into the environment.
        context.eval(language, source);

        // Get function handle from the script.
        function = context.eval(language, entrypoint);

    }

    public void addBindings(String language, Context context) {
        context.getBindings(language).putMember("polyHostAccess", new PolyglotHostAccess());
    }

    public String invoke(String arguments) {
        String resultString = new String();
        try {
            resultString = function.execute(arguments).toString();
        } catch (PolyglotException pe) {
            if (pe.isSyntaxError()) {
                 resultString = String.format("Error happens during parsing the polyglot function at line %s: %s", pe.getSourceLocation(), pe.getMessage());
            }
        } catch (Exception e) {
            resultString = String.format("Error while invoking polyglot function: %s", e.getMessage());
            System.err.println(resultString);
            e.printStackTrace(System.err);
        }

        return resultString;
    }
}
