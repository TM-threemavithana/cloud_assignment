import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

public class TopKVolatilityReducer extends Reducer<Text, Text, Text, Text> {

    private static class VolatilityRecord {
        final double avgRange;
        final String symbol;
        final String avgClose;
        final String maxHigh;
        final String minLow;
        final int tradingDays;

        VolatilityRecord(double avgRange, String symbol, String avgClose,
                String maxHigh, String minLow, int tradingDays) {
            this.avgRange = avgRange;
            this.symbol = symbol;
            this.avgClose = avgClose;
            this.maxHigh = maxHigh;
            this.minLow = minLow;
            this.tradingDays = tradingDays;
        }
    }

    @Override
    protected void reduce(Text year, Iterable<Text> values, Context context)
            throws IOException, InterruptedException {

        int topK = context.getConfiguration().getInt("topk.volatility.k", 10);

        PriorityQueue<VolatilityRecord> minHeap = new PriorityQueue<>(
                Comparator.comparingDouble(record -> record.avgRange));

        for (Text value : values) {
            String[] fields = value.toString().split("\\t");
            if (fields.length < 6) {
                context.getCounter("TopKVolatilityReducer", "MALFORMED_VALUES").increment(1);
                continue;
            }

            try {
                VolatilityRecord record = new VolatilityRecord(
                        Double.parseDouble(fields[0]),
                        fields[1],
                        fields[2],
                        fields[3],
                        fields[4],
                        Integer.parseInt(fields[5]));

                minHeap.offer(record);
                if (minHeap.size() > topK) {
                    minHeap.poll();
                }

            } catch (NumberFormatException ex) {
                context.getCounter("TopKVolatilityReducer", "PARSE_ERRORS").increment(1);
            }
        }

        List<VolatilityRecord> topRecords = new ArrayList<>(minHeap);
        topRecords.sort((left, right) -> Double.compare(right.avgRange, left.avgRange));

        int rank = 1;
        for (VolatilityRecord record : topRecords) {
            String output = String.format(
                    "%d\t%s\t%.4f\t%s\t%s\t%s\t%d",
                    rank,
                    record.symbol,
                    record.avgRange,
                    record.avgClose,
                    record.maxHigh,
                    record.minLow,
                    record.tradingDays);

            context.write(year, new Text(output));
            rank++;
        }
    }
}
