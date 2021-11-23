## The Argo
The project vision and initial experimental results are accessible in [Argo's README](lambda-manager/doc/Argo.md) page.

The project includes a number of repositories:
- [benchmarks](https://github.com/graalvm-argo/benchmarks/blob/main/README.md) - Contains multiple benchmarks in multiple languages that are used to benchmark the project;
- [lambda-proxy](https://github.com/graalvm-argo/core/blob/main/lambda-proxy/README.md) - Contains Serverless function wrappers that offer support for Native Image Isolates and Truffle Languages;
- [lambda-manager](https://github.com/graalvm-argo/core/blob/main/lambda-manager/README.md) - The Lambda Manager is a core component of the Argo architecture. It manages the resources of a local node by launching and terminating VMs where functions run;
- [cluster-manager](https://github.com/graalvm-argo/core/blob/main/cluster-manager/README.md) - The Cluster Manager is the component that overseas a number of Lambda Managers and decides where function invocations should be sent based on resource utilization (**prototype only**);
- [load-balancer](https://github.com/graalvm-argo/load-balancer/blob/main/README.md) - An NGINX-based network load balancer that balances load for a number of Cluster Managers (**prototype only**);
- [web-ui](https://github.com/graalvm-argo/web-ui/blob/master/README.md) - WebUI gives opportunities to users to upload functions, invoke functions, and see results in real time;
- [run](https://github.com/graalvm-argo/run/blob/main/README.md) - Run is a command-line tool for testing, plotting, running gates, updating project, and installing dependencies.
