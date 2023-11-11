# Frisbee - fast flat index for similarity search in Java

This repo contains my implementation of a flat index for similarity search.
It requires added vectors to be normalized and only supports a cosine
similarity.

Key characteristics:
* uses EJML matrix multiplication for similarity query
* supports matrix resize as items are added and removed, providing
  good performance throughout index lifetime
* it's very fast on modern hardware (see benchmarks below)

## Benchmarks

Comparing latency with FAISS - state-of-the-art similarity search library.

Task: query 3 closest vectors, 100k vectors in index, dim=1000.

Results:
* FAISS time: 80ms
* Frisbee time: 63ms (21% faster)

Environment: laptop with Apple M2 Max chip and 32GB RAM

## Usage

It's pretty straightforward, see one of the tests:
```java
// Arrange
BlockFlatIndex idx = new BlockFlatIndex(2);
idx.add(1 /*id*/, new float[]{0, 1});
idx.add(2 /*id*/, new float[]{0, -1});
idx.add(3 /*id*/, new float[]{-1, 0});
idx.add(4 /*id*/, new float[]{0.707107f, 0.707107f});
idx.add(5 /*id*/, new float[]{0.110432f, 0.993884f});

// Act
List<QueryRes> res = idx.query(2, new float[]{0, 1});

// Assert
assertThat(res).containsExactly(
    new QueryRes(1.0f /*similarity*/, 1 /*id*/),
    new QueryRes(0.993884f /*similarity*/, 5 /*id*/)
);
```