import java.io.IOException;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

/**
 * CryptoMapper — Hadoop Mapper for cryptocurrency yearly price analysis.
 *
 * Input: Each line of the CSV dataset (symbol, date, open, high, low, close,
 * network)
 * Output: Key = "SYMBOL,YEAR" | Value = "high,low,close"
 *
 * The mapper extracts the year from the date field and emits composite keys
 * so the reducer can aggregate daily prices by cryptocurrency per year.
 *
 * @author Cloud Computing Assignment — EE7222/EC7204
 */
public class CryptoMapper extends Mapper<LongWritable, Text, Text, Text> {

    private final Text outputKey = new Text();
    private final Text outputValue = new Text();

    /**
     * Map function — processes one CSV line at a time.
     *
     * @param key     Byte offset of the line in the file (ignored)
     * @param value   The CSV line as Text
     * @param context MapReduce context for emitting key-value pairs
     */
    @Override
    protected void map(LongWritable key, Text value, Context context)
            throws IOException, InterruptedException {

        String line = value.toString().trim();
        if (line.isEmpty())
            return;

        // Split CSV row (no quoted fields in this dataset)
        String[] fields = line.split(",");
        if (fields.length < 6)
            return;

        String symbol = fields[0].trim();
        String dateStr = fields[1].trim();

        // Skip the header row
        if (symbol.equalsIgnoreCase("symbol") || dateStr.equalsIgnoreCase("date")) {
            return;
        }

        try {
            // Extract year from ISO date format YYYY-MM-DD
            String year = dateStr.split("-")[0];

            // Parse price fields
            double high = Double.parseDouble(fields[3].trim());
            double low = Double.parseDouble(fields[4].trim());
            double close = Double.parseDouble(fields[5].trim());

            // Emit composite key and price values
            outputKey.set(symbol + "," + year);
            outputValue.set(high + "," + low + "," + close);
            context.write(outputKey, outputValue);

        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            // Increment a counter for monitoring bad records
            context.getCounter("CryptoMapper", "MALFORMED_RECORDS").increment(1);
        }
    }
}
