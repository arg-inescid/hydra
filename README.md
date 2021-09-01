## Lambda manager

The Lambda manager is a core component of the ARGO architecture. Manager is written in Java
using [Micronaut](https://guides.micronaut.io/index.html).

### Main components

The Manager consists of next main components:

- `Encoder` - Class for transforming username and lambda name to unique name, which is then used as the key for Function
  Storage.
- `Function Storage` - Class for storing meta-information about every registered function. Like ID, function name,
  available instances, created instances, active instances, opened HTTP connections...
- `Code Writer` - Class for storing binary code of servers writing them on the same disk as lambda manager.
- `Scheduler` - Class which is deciding which instance of lambda should we call.
- `Optimizer` - Class which is deciding whether to start a new instance of a lambda with as **Hotspot** or **VMM**.
  Server as next step in execution pipe (after `Encoder ` and `Scheduler` and before `Client`).
- `Client` - Class for making connections toward lambdas.
- `Lambda Manager` - Core class which is just a template while all implementations are kept in concrete implementations
  of Interfaces for all above classes.

### TODOs

- Maybe we want to share some parts from confluence page here, like overall scheme, use cases...
- Maybe we also want to share some parts from presentation here...
- Maybe to show some results here or in README.md from the top directory.

