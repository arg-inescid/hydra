package org.graalvm.argo.graalvisor.engine;

import java.util.HashMap;
import java.util.Map;
import org.graalvm.argo.graalvisor.base.PolyglotFunction;
import org.graalvm.argo.graalvisor.base.PolyglotLanguage;
import org.graalvm.argo.graalvisor.base.TruffleFunction;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;

import com.oracle.svm.graalvisor.utils.PolyglotHostAccess;

public class PolyglotEngine {

    /**
     * Each thread owns it function value, where polyglot functions execute.
     */
    private final ThreadLocal<Value> functionValue = new ThreadLocal<>();

    /**
     * Each sandbox has a corresponding truffle engine that should be used for the invocation.
     */
    private final Engine functionEngine = Engine.create();

    public String invoke(String functionName, String arguments) throws Exception {
        String resultString = new String();

        if (functionValue.get() == null) {
           PolyglotFunction polyglotFunction = FunctionStorage.FTABLE.get(functionName);

           if (polyglotFunction == null || !(polyglotFunction instanceof TruffleFunction)) {
              return String.format("{'Error': 'Function %s not registered or not truffle function!'}", functionName);
           }

           TruffleFunction truffleFunction = (TruffleFunction) polyglotFunction;
           Map<String, String> options = new HashMap<>();
            String language = polyglotFunction.getLanguage().toString();
            String javaHome = System.getenv("JAVA_HOME");

            if (javaHome == null) {
                System.err.println("JAVA_HOME not found in the environment. Polyglot functionality significantly limited.");
            } else {
                System.setProperty("org.graalvm.language.python.home", javaHome + "/languages/python");
                System.setProperty("org.graalvm.language.llvm.home", javaHome + "/languages/llvm");
                System.setProperty("org.graalvm.language.js.home", javaHome + "/languages/js");
            }

            if (polyglotFunction.getLanguage() == PolyglotLanguage.PYTHON) {
                // Necessary to allow python imports.
                options.put("python.ForceImportSite", "true");
                // Loading the virtual env with installed packages
                options.put("python.Executable", javaHome + "/graalvisor-python-venv/bin/python");
            }

            // Build context.
            Context context = Context.newBuilder().allowAllAccess(true).engine(functionEngine).options(options).build();
            System.out.println(String.format("[thread %s] Creating context %s", Thread.currentThread().getId(), context.toString()));

            // Host access to implement missing language functionalities.
            context.getBindings(language).putMember("polyHostAccess", new PolyglotHostAccess());

            // Evaluate source script to load function into the environment.
            context.eval(language, truffleFunction.getSource());

            // Get function handle from the script.
            functionValue.set(context.eval(language, polyglotFunction.getEntryPoint()));
        }

        try {
            resultString = functionValue.get().execute(arguments).toString();
        } catch (PolyglotException pe) {
            if (pe.isSyntaxError()) {
                 resultString = String.format("Error happens during parsingh the polyglot function at line %s: %s", pe.getSourceLocation(), pe.getMessage());
            }
        } catch (Exception e) {
            resultString = String.format("Error while invoking polyglot function: %s", e.getMessage());
            System.err.println(resultString);
            e.printStackTrace(System.err);
        }

        return resultString;
    }
}
