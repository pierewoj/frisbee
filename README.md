# Frisbee - fast flat index for similarity search in Java

This repo contains my implementation of a flat index for similarity search.
It requires added vectors to be normalized and only supports a cosine
similarity measure.

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

