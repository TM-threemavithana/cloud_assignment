import java.io.IOException;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

/**
 * CryptoReducer — Hadoop Reducer for cryptocurrency yearly price analysis.
 *
 * Input: Key = "SYMBOL,YEAR" | Values = iterable of value strings
 * Output: Tab-separated row: Symbol Year AvgClose MaxHigh MinLow AvgDailyRange
 * TradingDays
 *
 * Handles two formats of input values:
 * - Raw from Mapper (3 fields): "high,low,close"
 * - Pre-aggregated from Combiner (5 fields):
 * "totalClose,maxHigh,minLow,totalRange,count"
 *
 * Aggregation metrics:
 * - Average Close Price → mean of all daily closing prices
 * - Maximum High → absolute peak price for the year
 * - Minimum Low → absolute lowest price for the year
 * - Average Daily Range → mean of (High - Low) per day — volatility proxy
 * - Trading Days → number of valid data points
 *
 * @author Cloud Computing Assignment — EE7222/EC7204
 */
public class CryptoReducer extends Reducer<Text, Text, Text, Text> {

    private final Text result = new Text();

    /**
     * Reduce function — aggregates daily price data for one Symbol+Year.
     *
     * @param key     Composite key "SYMBOL,YEAR"
     * @param values  Daily price values (raw or pre-aggregated)
     * @param context MapReduce context for emitting the final result
     */
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
