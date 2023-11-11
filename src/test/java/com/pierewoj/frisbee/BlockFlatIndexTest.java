package com.pierewoj.frisbee;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BlockFlatIndexTest {
    @Test
    public void test() {
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