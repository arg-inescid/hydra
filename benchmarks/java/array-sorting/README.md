## Description

[Micronaut](https://guides.micronaut.io/index.html) server with operation of merging two arrays into one sorted array as
a workload.

### Language

Written in **Java** using [Micronaut](https://guides.micronaut.io/index.html).

### Build tool

Built with **Gradle**.

### Functions

```python
def intersect(array_size):
    # Generate arrays with specified number of elements (array_size).
    array1 = generate_array(array_size)
    array2 = generate_array(array_size)
    # Sort two arrays.
    array1 = sorted(array1)
    array2 = sorted(array2)
    # Merge them into one sorted array.
    array3 = merge(array1, array2)
    # Check if array is sorted.
    result = is_sorted(array3)
    return result
```