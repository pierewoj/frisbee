package com.pierewoj.frisbee;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.ejml.simple.SimpleMatrix;

import java.util.*;
import java.util.stream.IntStream;

/**
 * Fast flat vector index. It stores vector data in a matrix and uses matrix multiplication
 * for k-nearest query. It supports matrix resize (both increasing matrix size and decreasing)
 * during index lifetime, as items are added and removed. This results in very good performance on modern CPU
 * on different index sizes.
 */
public class BlockFlatIndex {
    private static final int START_BLOCK_SIZE = 8;

    /**
     * Maps id to the location of an iteam in a block
     */
    private final BiMap<Integer, Integer> idToBlockLocationMap = HashBiMap.create();
    /**
     * Collection of free rows in a block, sorted by row number increasing
     */
    private final PriorityQueue<Integer> freeBlockRows = new PriorityQueue<>();
    /**
     * Set of used rows in a block
     */
    private final TreeSet<Integer> indexesUsed = new TreeSet<>();
    /**
     * Dimension of stored vectors. Each vector in an index must use this dimension.
     */
    private final int dim;

    /**
     * Block, primary storage of the index. It's a float matrix.
     */
    private SimpleMatrix block;

    /**
     * Build index for vectors.
     * @param dim dimension, each added vector will need to have exactly dim elements.
     */
    public BlockFlatIndex(int dim) {
        block = new SimpleMatrix(START_BLOCK_SIZE, dim);
        for (int i = 0; i < block.getNumRows(); i++) {
            freeBlockRows.add(i);
        }
        this.dim = dim;
    }

    /**
     * Adds a new vector to the index. Throws IllegalArgumentException if vector with a given id
     * already exists or if there is a dimensionality mismatch.
     * @param id id of a vector, has to be unique across all vectors in the index
     * @param vec vector data, normalized
     */
    public synchronized void add(int id, float[] vec) {
        if (idToBlockLocationMap.get(id) != null) {
            throw new IllegalArgumentException("Item with id already exists");
        }
        Integer idx = freeBlockRows.poll();
        if (idx == null) {
            int oldCapa = this.block.getNumRows();
            // resize
            this.block.reshape(this.block.getNumRows() * 2, dim);
            this.freeBlockRows.addAll(IntStream.range(oldCapa, 2 * oldCapa).boxed().toList());
            idx = freeBlockRows.poll();
        }
        if (vec.length != dim) {
            throw new IllegalArgumentException("Vec does not match dim");
        }
        block.setRow(idx, new SimpleMatrix(vec));
        idToBlockLocationMap.put(id, idx);
        indexesUsed.add(idx);
    }

    /**
     * Retrurns vector data by id.
     * @param id vector id
     * @return vector data or null of no vector with specified id exists
     */
    public synchronized float[] get(int id) {
        Integer idx = idToBlockLocationMap.get(id);
        if (idx == null) {
            return null;
        }
        float[] res = new float[dim];
        for (int i = 0; i < dim; i++) {
            res[i] = (float) block.get(idx, i);
        }
        return res;
    }

    /**
     * Removes vector by id
     * @param id vector id
     * @return true if vector was removed, false if it did not exist
     */
    public synchronized boolean delete(int id) {
        Integer idx = idToBlockLocationMap.get(id);
        if (idx == null) {
            return false;
        }
        idToBlockLocationMap.remove(id);
        freeBlockRows.add(idx);
        indexesUsed.remove(idx);
        compact();
        return true;
    }

    /**
     * Query for k closest vectors present in index, based on the cosine similarity.
     * Assumes all vectors in index have equal length (this is implied if vectors are normalized).
     * @param k
     * @param vec
     * @return
     */
    public synchronized List<QueryRes> query(int k, float[] vec) {

        List<Map.Entry<Integer, Integer>> indexes = idToBlockLocationMap
                .entrySet()
                .stream()
                .sorted(Comparator.comparingInt(Map.Entry::getValue))
                .toList();
        SimpleMatrix vecM = new SimpleMatrix(vec);
        SimpleMatrix dotProducts = block.mult(vecM);
        PriorityQueue<QueryRes> res = new PriorityQueue<>(Comparator.comparingDouble(QueryRes::similarity));
        for (int i = 0; i < block.getNumRows(); i++) {
            double similarity = dotProducts.get(i);
            Integer id = idToBlockLocationMap.inverse().get(i);
            if (id == null) {
                continue;
            }
            res.add(new QueryRes((float) similarity, id));
            while (res.size() > k) {
                res.remove();
            }
        }
        return res.stream().sorted(Comparator.comparing(QueryRes::similarity).reversed()).toList();
    }

    private void compact() {
        int curBlockSize = block.getNumRows();
        int curBlockUsed = indexesUsed.size();

        if (curBlockSize <= START_BLOCK_SIZE) {
            // not resized yet
            return;
        }
        if (curBlockUsed * 4 > curBlockSize) {
            // util now low enough
            return;
        }

        int numRowsAfterCompaction = curBlockSize / 2;
        int maxAllowedIndexAfterCompaction = numRowsAfterCompaction - 1;
        int currentMaxIndex = indexesUsed.last();
        if (currentMaxIndex < maxAllowedIndexAfterCompaction) {
            // compact
            this.block = this.block.rows(0, numRowsAfterCompaction);
            // need to fix free indexes
            this.freeBlockRows.clear();
            IntStream.range(0, numRowsAfterCompaction).parallel()
                    .filter(blockIdx -> !indexesUsed.contains(blockIdx))
                    .forEach(this.freeBlockRows::add);
            return;
        }

        int numberOfCompactionsLeft = 10;
        while (numberOfCompactionsLeft-- >= 0) {
            int indexToSwap = indexesUsed.last();
            if (indexToSwap < maxAllowedIndexAfterCompaction) {
                // no move needed, compaction should succeed on a next invocation
            }
            Integer newIndex = freeBlockRows.poll();
            if (newIndex == null) {
                throw new IllegalStateException("This should never happen, likely a bug");
            }
            // we have to copy from indexToSwap to newIndex and update free and other collections
            this.block.setRow(newIndex, block.getRow(indexToSwap));
            this.freeBlockRows.add(indexToSwap);
            this.indexesUsed.remove(indexToSwap);
            this.indexesUsed.add(newIndex);
            int id = idToBlockLocationMap.inverse().get(indexToSwap);
            idToBlockLocationMap.inverse().remove(indexToSwap);
            idToBlockLocationMap.put(id, newIndex);
        }
    }

    int getBlockCapacity() {
        return block.getNumRows();
    }
}
