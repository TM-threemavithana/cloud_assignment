import java.io.IOException;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
public class CryptoMapper extends Mapper<LongWritable, Text, Text, Text> {

    private final Text outputKey = new Text();
    private final Text outputValue = new Text();

    
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
