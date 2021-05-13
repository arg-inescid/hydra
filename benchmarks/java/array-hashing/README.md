## Description

[Micronaut](https://guides.micronaut.io/index.html) server with operation of finding the longest harmonious sequence (we
define a harmonious array as an array where the difference between its maximum value and its minimum value is exactly
**1**) as workload.

### Language

Written in **Java** using [Micronaut](https://guides.micronaut.io/index.html).

### Build tool

Built with **Gradle**.

### Functions

```python
def findLHS(array_size):
    # Generate array with specified number of elements (array_size).
    array = generate_array(array_size)
    # Add all elements into hash map.
    hash_map = add_all(array)
    # Find longest harmonious sequence.
    result = longest_sequnce(hash_map)
    return result
```