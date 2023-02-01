package org.graalvm.argo.graalvisor.engine;

import static org.graalvm.argo.graalvisor.utils.IsolateUtils.copyString;
import static org.graalvm.argo.graalvisor.utils.IsolateUtils.retrieveString;
import java.util.HashMap;
import java.util.Map;
import org.graalvm.argo.graalvisor.SubstrateVMProxy;
import org.graalvm.argo.graalvisor.base.IsolateObjectWrapper;
import org.graalvm.argo.graalvisor.base.PolyglotFunction;
import org.graalvm.argo.graalvisor.base.PolyglotLanguage;
import org.graalvm.nativeimage.*;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.SourceSection;
import org.graalvm.polyglot.Value;

import com.oracle.svm.graalvisor.types.GuestIsolateThread;

// TODO - split into JavaNativeLibraryEngine and TruffleEngine
public class PolyglotEngine {

    /**
     * Each thread owns it truffle context, where polyglot functions execute.
     */
    private ThreadLocal<Context> context = new ThreadLocal<>();

    /**
     * Each context has a corresponding truffle function that should be used for the invocation.
     */
    private Value function;

    @SuppressWarnings("unused")
    @CEntryPoint
    private static void installSourceCode(@CEntryPoint.IsolateThreadContext IsolateThread workingThread,
                    ObjectHandle functionName,
                    ObjectHandle entryPoint,
                    ObjectHandle language,
                    ObjectHandle sourceCode) {
        FunctionStorage.FTABLE.put(retrieveString(functionName), new PolyglotFunction(retrieveString(functionName), retrieveString(entryPoint), retrieveString(language), retrieveString(sourceCode)));
    }

    public String invoke(String functionName, String arguments) throws Exception {
        PolyglotFunction guestFunction = FunctionStorage.FTABLE.get(functionName);
        String resultString = new String();

        if (guestFunction == null) {
            return String.format("{'Error': 'Function %s not registered!'}", functionName);
        }

        if (context.get() == null) {
            Map<String, String> options = new HashMap<>();
            String language = guestFunction.getLanguage().toString();
            String javaHome = System.getenv("JAVA_HOME");

            if (javaHome == null) {
                System.err.println("JAVA_HOME not found in the environment. Polyglot functionality significantly limited.");
            } else {
                System.setProperty("org.graalvm.language.python.home", javaHome + "/languages/python");
                System.setProperty("org.graalvm.language.llvm.home", javaHome + "/languages/llvm");
                System.setProperty("org.graalvm.language.js.home", javaHome + "/languages/js");
            }

            if (guestFunction.getLanguage() == PolyglotLanguage.PYTHON) {
                // Necessary to allow python imports.
                options.put("python.ForceImportSite", "true");
                // Loading the virtual env with installed packages
                options.put("python.Executable", javaHome + "/graalvisor-python-venv/bin/python");
            }

            // Build context.
            context.set(Context.newBuilder().allowAllAccess(true).engine(guestFunction.getTruffleEngine()).options(options).build());
            System.out.println(String.format("[thread %s] Creating context %s", Thread.currentThread().getId(), context.get().toString()));

            // Host access to implement missing language functionalities.
            context.get().getBindings(language).putMember("polyHostAccess", new PolyglotHostAccess());

            // Evaluate source script to load function into the environment.
            context.get().eval(language, guestFunction.getSource());

            // Get function handle from the script.
            function = context.get().eval(language, guestFunction.getEntryPoint());
        }

        try {
            resultString = function.execute(arguments).toString();
        } catch (PolyglotException pe) {
            if (pe.isSyntaxError()) {
                 SourceSection location = pe.getSourceLocation();
                 resultString = String.format("Error happens during parsing/ polyglot function at line %s: %s", location, pe.getMessage());
            }
        } catch (Exception e) {
            System.err.println("Error happens during invoking polyglot function: ");
            resultString = String.format("Error happens during invoking polyglot function:: %s", e.getMessage());
        }

        return resultString;
    }

    public String invoke(IsolateObjectWrapper workingIsolate, String functionName, String jsonArguments) throws Exception {
        PolyglotFunction guestFunction = FunctionStorage.FTABLE.get(functionName);
        if (guestFunction == null) {
            return String.format("{'Error': 'Function %s not registered!'}", functionName);
        } else if (guestFunction.getLanguage().equals(PolyglotLanguage.JAVA)) {
            GuestIsolateThread guestThread = (GuestIsolateThread) workingIsolate.getIsolateThread();
            return guestFunction.getGraalVisorAPI().invokeFunction(guestThread, guestFunction.getEntryPoint(), jsonArguments);
        } else {
            IsolateThread workingThread = workingIsolate.getIsolateThread();
            return retrieveString(SubstrateVMProxy.invoke(workingThread, CurrentIsolate.getCurrentThread(), copyString(workingThread, functionName), copyString(workingThread, jsonArguments)));
        }
    }

    public IsolateObjectWrapper createIsolate(String functionName) {
        PolyglotFunction polyglotFunction = FunctionStorage.FTABLE.get(functionName);
        if (polyglotFunction == null)
            return null;
        else if (polyglotFunction.getLanguage().equals(PolyglotLanguage.JAVA)) {
            GuestIsolateThread guestThread = polyglotFunction.getGraalVisorAPI().createIsolate();
            return new IsolateObjectWrapper(Isolates.getIsolate(guestThread), guestThread);
        } else {
            // create a new isolate and setup configurations in that isolate.
            IsolateThread isolateThread = Isolates.createIsolate(Isolates.CreateIsolateParameters.getDefault());
            Isolate isolate = Isolates.getIsolate(isolateThread);
            // initialize source code into isolate
            installSourceCode(isolateThread,
                            copyString(isolateThread, functionName),
                            copyString(isolateThread, polyglotFunction.getEntryPoint()),
                            copyString(isolateThread, polyglotFunction.getLanguage().name()),
                            copyString(isolateThread, polyglotFunction.getSource()));
            return new IsolateObjectWrapper(isolate, isolateThread);
        }
    }

    public void tearDownIsolate(String functionName, IsolateObjectWrapper workingIsolate) {
        if (functionName != null && workingIsolate != null && FunctionStorage.FTABLE.containsKey(functionName)) {
            if (FunctionStorage.FTABLE.get(functionName).getLanguage().equals(PolyglotLanguage.JAVA)) {
                FunctionStorage.FTABLE.get(functionName).getGraalVisorAPI().tearDownIsolate((GuestIsolateThread) workingIsolate.getIsolateThread());
            } else {
                Isolates.tearDownIsolate(workingIsolate.getIsolateThread());
            }
        }
    }
}
