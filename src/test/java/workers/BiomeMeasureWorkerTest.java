package workers;

import com.lucalabs.naturescompass.NaturesCompass;
import com.lucalabs.naturescompass.utils.BiomeUtils;
import com.lucalabs.naturescompass.workers.BiomeMeasureWorker;
import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BiomeMeasureWorkerTest {

    List<int[][]> testCases = List.of(new int[][]{
                    {0, 0, 0, 0, 0, 0, 0, 0, 0},
                    {0, 0, 1, 1, 1, 0, 0, 0, 0},
                    {0, 1, 1, 1, 1, 1, 0, 0, 0},
                    {0, 1, 1, 1, 1, 1, 1, 0, 0},
                    {0, 0, 1, 1, 2, 1, 1, 1, 0},
                    {0, 0, 0, 1, 1, 1, 1, 1, 0},
                    {0, 0, 0, 0, 1, 1, 1, 1, 0},
                    {0, 0, 0, 0, 0, 1, 1, 1, 0},
                    {0, 0, 0, 0, 0, 0, 0, 0, 0}
            },
            new int[][]{
                    {0, 0, 0, 0, 0, 0, 0, 0, 0},
                    {0, 0, 0, 0, 0, 0, 0, 0, 0},
                    {0, 0, 1, 1, 0, 0, 0, 0, 0},
                    {0, 0, 1, 1, 1, 0, 0, 0, 0},
                    {0, 0, 1, 1, 2, 1, 0, 0, 0},
                    {0, 0, 0, 1, 1, 1, 1, 0, 0},
                    {0, 0, 0, 0, 1, 1, 1, 1, 0},
                    {0, 0, 0, 0, 0, 1, 1, 1, 0},
                    {0, 0, 0, 0, 0, 0, 0, 0, 0}
            });

    // using an array as a key feels very, very wrong. I would have used a list of pairs, but Java apparently has no
    // built-in pairs. Tf kind of language doesn't have pairs?? But who cares, it's a test.
    Map<int[][], BiomeUtils.BoundingBox> solutions = Map.of(
            testCases.get(0), new BiomeUtils.BoundingBox(
                    new BlockPos(-3 * 16, 0, -3 * 16),
                    new BlockPos(3 * 16, 0, 3 * 16)
            ),
            testCases.get(1), new BiomeUtils.BoundingBox(
                    new BlockPos(-2 * 16, 0, -3 * 16),
                    new BlockPos(3 * 16, 0, 2 * 16)
            ));


    @BeforeAll
    static void beforeAll() {
        SharedConstants.createGameVersion();
        Bootstrap.initialize();
    }

    @Test
    void testBiomeMeasurement() throws IOException {
        for (int[][] testCase : testCases) {
            try (
                    MockedStatic<BiomeUtils> biomeUtils = Mockito.mockStatic(BiomeUtils.class);
                    ServerWorld serverWorldM = mock(ServerWorld.class);
            ) {
                BlockPos origin = new BlockPos(0, 0, 0);

                when(serverWorldM.getBottomY()).thenReturn(-64);

                biomeUtils.when(() -> BiomeUtils.isBiomeAtPositionEqual(Mockito.any(), Mockito.any(), Mockito.any()))
                        .thenAnswer(invocation -> {
                            Vec3i pos = invocation.getArgument(2);

                            int x = pos.getX() / 16 + 4;
                            int z = pos.getZ() / 16 + 4;

                            if (x >= testCase.length || z >= testCase.length || x < 0 || z < 0) {
                                return false;
                            }

                            return testCase[z][x] > 0;
                        });

                AtomicBoolean callbackCalled = new AtomicBoolean(false);
                BiomeMeasureWorker w = new BiomeMeasureWorker(serverWorldM, origin, b -> {
                    callbackCalled.set(true);
                    BiomeUtils.BoundingBox correct = solutions.get(testCase);
                    Assertions.assertEquals(b.nw().getX(), correct.nw().getX());
                });

                while (w.doWork()) ;

                Assertions.assertTrue(callbackCalled.get());
            }
        }
    }
}
