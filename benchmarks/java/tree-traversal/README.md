## Description

[Micronaut](https://guides.micronaut.io/index.html) server with operation of finding max tree depth as workload.

### Language

Written in **Java** using [Micronaut](https://guides.micronaut.io/index.html).

### Build tool

Built with **Gradle**.

### Functions

```python
def max_tree_depth(num_nodes):
    # Generate tree with specified number of nodes (num_nodes).
    root = generate_tree(num_nodes)
    # Traverse tree in level order and remember level for each leaf.
    leaf_levels = traverse(root)
    # Find max_level between all leaf levels.
    max_level = max(leaf_levels)
    return max_level
```