package com.atenishev.storagestat;

import android.net.Uri;
import android.support.annotation.NonNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.TreeMap;

public class ScanData {
    static final int AVERAGE_FILESIZE_CHUNK_MAX = 10; // this is package-private

    public static class FileInfo implements Comparable {
        public final long size;
        public final String name;
        public FileInfo(final long size, final String name) {
            this.size = size;
            this.name = name;
        }

        @Override
        public int compareTo(@NonNull Object o) {
            if( !(o instanceof FileInfo) ) {
                throw new ClassCastException("Incompatible object types");
            }
            final FileInfo other = (FileInfo) o;
            if( other == this ) {
                return 0;
            }

            if( other.size == this.size ) {
                return this.name.compareTo(other.name);
            }

            return this.size < other.size ? -1 : 1;
        }

        @Override
        public int hashCode() {
            int result = name.hashCode();
            result = 31 * result + (int)(size^(size>>>32));
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if( this == obj ) {
                return true;
            }
            if( !(obj instanceof FileInfo) ) {
                return false;
            }

            final FileInfo other = (FileInfo) obj;
            return size == other.size && name.equals(other.name);
        }
    }

    public ScanData() {
    }
    // Average file size
    private long averageFileSize;
    // auxillary data
    private long averageFileSizeChunk;
    private long averageFileSizeChunkSize;

    // Names and sizes of biggest files
    private PriorityQueue<FileInfo> biggestFiles = new PriorityQueue<>(Constants.BIGGEST_FILES_NUM * 10, Collections.reverseOrder());

    // most frequent file extensions (with their frequencies)
    private HashMap<String, Integer> extensions = new HashMap<>();
    private TreeMap<Integer, HashSet<String>> extensionsSecondIndex = new TreeMap<>(Collections.reverseOrder());

    public void process(final String name, final String shortname, final long size) {
        biggestFiles.add(new FileInfo(size, name));
        checkReduceBiggestFilesSize();

        //FIXME: last path segm returns filename
        final int ext_break = shortname.lastIndexOf('.');
        if( ext_break != -1 ) {
            final String extension = shortname.substring(ext_break + 1);
            if (extensions.containsKey(extension)) {
                final int currentFreq = extensions.get(extension);
                extensions.put(extension, currentFreq + 1);

                final HashSet<String> exts = extensionsSecondIndex.get(currentFreq);
                exts.remove(extension);
                if (!extensionsSecondIndex.containsKey(currentFreq + 1)) {
                    extensionsSecondIndex.put(currentFreq + 1, new HashSet<>());
                }
                extensionsSecondIndex.get(currentFreq + 1).add(extension);
            } else {
                extensions.put(extension, 1);
                if (!extensionsSecondIndex.containsKey(1)) {
                    extensionsSecondIndex.put(1, new HashSet<>());
                }
                extensionsSecondIndex.get(1).add(extension);
            }
        }

        if( averageFileSizeChunkSize < AVERAGE_FILESIZE_CHUNK_MAX ) {
            averageFileSizeChunkSize++;
            averageFileSizeChunk += size;
        } else {
            averageFileSize += (averageFileSizeChunk/averageFileSizeChunkSize);
            averageFileSize /= 2;
            averageFileSizeChunkSize = 0;
            averageFileSizeChunk = 0;
        }
    }

    private void checkReduceBiggestFilesSize() {
        if( biggestFiles.size() > Constants.BIGGEST_FILES_NUM * 10 ) {
            // just replace queue
            PriorityQueue<FileInfo> biggestFilesNew = new PriorityQueue<>(Constants.BIGGEST_FILES_NUM * 10, Collections.reverseOrder());
            for( int i = 0; i < Constants.BIGGEST_FILES_NUM; ++i ) {
                biggestFilesNew.add(biggestFiles.poll());
            }
            biggestFiles = biggestFilesNew;
        }
    }

    public long getAverageFileSize() {
        if( averageFileSizeChunkSize == 0 ) {
            return averageFileSize;
        }
        return ( averageFileSize + ( averageFileSizeChunk / averageFileSizeChunkSize ) ) / 2;
    }

    public FileInfo[] getBiggestFiles() {
        final PriorityQueue<FileInfo> fromBiggestFiles = new PriorityQueue<>(biggestFiles);
        final FileInfo[] result = new FileInfo[Constants.BIGGEST_FILES_NUM];
        int count = 0;
        while( count < Constants.BIGGEST_FILES_NUM && fromBiggestFiles.size() > 0 ) {
            final FileInfo next = fromBiggestFiles.poll();
            result[count] = next;
            ++count;
        }
        return result;
    }

    public FileInfo[] getMostFrequentExtensions() {
        final FileInfo[] result = new FileInfo[Constants.FREQ_EXTS_NUM];
        int count = 0;
        for(Map.Entry<Integer, HashSet<String>> entry : extensionsSecondIndex.entrySet()) {
            final Integer freq = entry.getKey();
            final HashSet<String> exts = entry.getValue();
            for( String ext : exts ) {
                result[count] = new FileInfo(freq, ext);
                ++count;
                if( count >= Constants.FREQ_EXTS_NUM ) {
                    break;
                }
            }

            if( count >= Constants.FREQ_EXTS_NUM ) {
                break;
            }
        }
        return result;
    }
}
