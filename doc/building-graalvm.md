Argo requires a GraalVM build with support for, NativeImage, Virtualization, and Truffle languages.

After checking out both graal and graal-enterprise, use the following command to build graalvm:

`mx -p graal-enterprise/vm-enterprise --env svm_all build`

*Note: due to an open issue, graal-enterprise should be at the following commit `94e64134b2`.*

Remember that it might be useful to run the following command before starting the compilation:

`mx -p graal-enterprise/vm-enterprise --env svm_all sforceimports`
