package com.pierewoj.frisbee;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BlockFlatIndexTest {
    @Test
    public void test() {
        // Arrange
        BlockFlatIndex idx = new BlockFlatIndex(2);
        idx.add(1, new float[]{0, 1});
        idx.add(2, new float[]{0, -1});
        idx.add(3, new float[]{-1, 0});
        idx.add(4, new float[]{1, 0});
        idx.add(5, new float[]{0, 0.5f});

        // Act
        List<QueryRes> res = idx.query(2, new float[]{0, 2});

        // Assert
        assertThat(res).containsExactly(
                new QueryRes(1, 5),
                new QueryRes(2, 1)
        );
    }

    @Test
    public void test_resizeWorks() {
        // Arrange
        BlockFlatIndex idx = new BlockFlatIndex(2);
        int startCapacity = idx.getBlockCapacity();

        // Act
        for (int i = 0; i < startCapacity * 2; i++) {
            idx.add(i, new float[]{i, i});
        }

        // Assert
        assertThat(idx.getBlockCapacity()).isGreaterThan(startCapacity);
    }

    @Test
    public void test_downscaleWorks() {
        // Arrange
        BlockFlatIndex idx = new BlockFlatIndex(2);
        int startCapacity = idx.getBlockCapacity();

        // Act
        for (int i = 0; i < startCapacity * 2; i++) {
            idx.add(i, new float[]{i, i});
        }

        for (int i = 0; i < startCapacity * 2; i++) {
            idx.delete(i);
        }

        // Assert
        assertThat(idx.getBlockCapacity()).isEqualTo(startCapacity);
    }

    @Test
    public void test_delete() {
        // Arrange
        BlockFlatIndex idx = new BlockFlatIndex(2);
        idx.add(1, new float[]{0, 1});

        // Act
        idx.delete(1);

        // Assert
        idx.add(2, new float[]{0, -1});
        List<QueryRes> res = idx.query(2, new float[]{0, 2});
        assertThat(res).containsExactlyInAnyOrder(
                new QueryRes(-2, 2)
        );
    }

    @Test
    public void test_get() {
        // Arrange
        BlockFlatIndex idx = new BlockFlatIndex(2);
        idx.add(1, new float[]{0, 1});

        // Act
        float[] res = idx.get(1);

        // Assert
        assertThat(res).isEqualTo(new float[]{0, 1});
    }

    @Test
    public void test_get_ifDoesNotExist() {
        // Arrange
        BlockFlatIndex idx = new BlockFlatIndex(2);
        idx.add(1, new float[]{0, 1});

        // Act
        float[] res = idx.get(2);

        // Assert
        assertThat(res).isNull();
    }
}