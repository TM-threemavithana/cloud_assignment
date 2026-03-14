import java.io.IOException;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

public class CryptoReducer extends Reducer<Text, Text, Text, Text> {

    private final Text result = new Text();


    @Override
    protected void reduce(Text key, Iterable<Text> values, Context context)
            throws IOException, InterruptedException {

        // Aggregation accumulators
        double totalClose = 0.0;
        double totalRange = 0.0;
        double maxHigh = Double.NEGATIVE_INFINITY;
        double minLow = Double.POSITIVE_INFINITY;
        int count = 0;

        for (Text val : values) {
            String[] parts = val.toString().split(",");

            try {
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
                    // Combiner output: totalClose, maxHigh, minLow, totalRange, count
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
            } catch (NumberFormatException e) {
                context.getCounter("CryptoReducer", "PARSE_ERRORS").increment(1);
            }
        }

        if (count == 0)
            return;

        // Compute final averages
        double avgClose = totalClose / count;
        double avgRange = totalRange / count;

        // Parse composite key into Symbol and Year
        String[] keyParts = key.toString().split(",");
        String symbol = keyParts[0];
        String year = keyParts[1];

        // Format: Symbol Year AvgClose MaxHigh MinLow AvgDailyRange TradingDays
        String output = String.format("%s\t%s\t%.4f\t%.4f\t%.4f\t%.4f\t%d",
                symbol, year, avgClose, maxHigh, minLow, avgRange, count);

        result.set("");
        context.write(new Text(output), result);
    }
}
