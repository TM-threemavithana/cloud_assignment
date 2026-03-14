import java.io.IOException;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

public class CryptoCombiner extends Reducer<Text, Text, Text, Text> {

    private final Text outputValue = new Text();

    @Override
    protected void reduce(Text key, Iterable<Text> values, Context context)
            throws IOException, InterruptedException {

        double totalClose = 0.0;
        double totalRange = 0.0;
        double maxHigh = Double.NEGATIVE_INFINITY;
        double minLow = Double.POSITIVE_INFINITY;
        int count = 0;

        for (Text val : values) {
            String[] parts = val.toString().split(",");

            if (parts.length == 3) {
                // Raw mapper output: high, low, close
                double high = Double.parseDouble(parts[0]);
                double low = Double.parseDouble(parts[1]);
                double close = Double.parseDouble(parts[2]);

                totalClose += close;
                totalRange += (high - low);
                if (high > maxHigh)
                    maxHigh = high;
                if (low < minLow)
                    minLow = low;
                count++;

            } else if (parts.length == 5) {
                // Previously combined output: totalClose, maxHigh, minLow, totalRange, count
                totalClose += Double.parseDouble(parts[0]);
                double h = Double.parseDouble(parts[1]);
                double l = Double.parseDouble(parts[2]);
                totalRange += Double.parseDouble(parts[3]);
                int c = Integer.parseInt(parts[4]);

                if (h > maxHigh)
                    maxHigh = h;
                if (l < minLow)
                    minLow = l;
                count += c;
            }
        }

        if (count == 0)
            return;

        // Emit partial aggregation: totalClose,maxHigh,minLow,totalRange,count
        outputValue.set(totalClose + "," + maxHigh + "," + minLow + "," + totalRange + "," + count);
        context.write(key, outputValue);
    }
}
