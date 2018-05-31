package com.atenishev.storagestat;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ScanDataTest {

    private ScanData data;
    @Before
    public void setUp() throws Exception {
        data = new ScanData();
    }

    @Test
    public void process() {
        final ScanData.FileInfo[] initial = {
                new ScanData.FileInfo(10, "A"),
                new ScanData.FileInfo(100, "B"),
                new ScanData.FileInfo(120, "C"),
                new ScanData.FileInfo(130, "D"),
                new ScanData.FileInfo(140, "E"),
                new ScanData.FileInfo(210, "F"),
                new ScanData.FileInfo(310, "G"),
                new ScanData.FileInfo(410, "H"),
                new ScanData.FileInfo(510, "I"),
                new ScanData.FileInfo(710, "J"),
                new ScanData.FileInfo(910, "K"),
                new ScanData.FileInfo(1210, "L"),
                new ScanData.FileInfo(1410, "M"),
                new ScanData.FileInfo(5110, "N"),
                new ScanData.FileInfo(6310, "O"),
                new ScanData.FileInfo(6310, "P"),
                new ScanData.FileInfo(7510, "Q"),
                new ScanData.FileInfo(75610, "R"),
        };

        for( final ScanData.FileInfo item : initial ) {
            data.process(item.name, item.name, item.size);
        }

        final ScanData.FileInfo[] biggestFiles = data.getBiggestFiles();
        assertNotNull(biggestFiles);
        assertEquals(Constants.BIGGEST_FILES_NUM, biggestFiles.length);
        for( int i = 0; i < Constants.BIGGEST_FILES_NUM; ++i ) {
            assertEquals(initial[initial.length - 1 - i], biggestFiles[i]);
        }
    }

    @Test
    public void getAverageFileSize1() {
        final long[] sizes = { 2, 4, 6, 8, 15, 25, 40};
        long expected = 0;
        for( long size : sizes ) {
            data.process("none", "none", size);
            expected += size;
        }
        expected = expected / sizes.length;

        assertEquals(expected, data.getAverageFileSize());
    }

    @Test
    public void getAverageFileSize2() {
        // this case is different from above
        long expected = 0;
        long size = 5;
        final long total_count = ScanData.AVERAGE_FILESIZE_CHUNK_MAX + 5;
        for( int i = 0; i < total_count; ++i ) {
            data.process("none", "none", size);
            expected += size;
            size += 8;
        }
        expected = expected / total_count;
        assertEquals(expected, data.getAverageFileSize());
    }
}