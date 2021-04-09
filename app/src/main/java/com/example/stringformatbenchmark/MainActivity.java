package com.example.stringformatbenchmark;

import android.util.Log;
import android.view.WindowManager;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
  }

  @Override
  protected void onResume() {
    super.onResume();

    int mSystemId, mNetworkId, mBasestationId;
    for (int i=0; i<0xffff; i++) {
      mSystemId = mNetworkId = mBasestationId = i;
      String t11 = String.format("%04x%04x%04x", mSystemId, mNetworkId,  mBasestationId);
      String t22 = formatSimple("%04x%04x%04x", mSystemId, mNetworkId,  mBasestationId);
      if (!t11.equals(t22)) {
        Log.e("Benchmark", "Wrong ID!!!");
      }
    }
    Log.i("Benchmark", "TGIF");

    String msg = "";
    for (int n=1; n<0xfffffff; n*=10) {
      msg += unitTest(n);
    }
    msg += unitTest(0xfffffff);

    ((TextView)findViewById(R.id.helloworld)).setText(msg);

    Log.i("Benchmark", "Validating IDs");
    for (int i=0; i<=0xfffffff; i++) {
      String t11 = String.format("%07x", i);
      String t22 = formatSimple("%07x", i);
      String t33 = Integer.toHexString(0x10000000 | i).substring(1);
      if (!t11.equals(t22)) {
        Log.e("Benchmark", "Wrong ID detected with formatSimple");
        break;
      }
      if (!t11.equals(t33)) {
        Log.e("Benchmark", "Wrong ID detected with toHexString+substring");
        break;
      }
      if ((i & 0xffffff) == 0xffffff) Log.i("Benchmark", String.format("Progress: %x/%x (%f%%)", i, 0xfffffff, i * 100.0 / 0xfffffff));
    }
    Log.i("Benchmark", "IDs validated");
  }

  protected String unitTest(int n) {
    long s1 = System.nanoTime();
    String t1 = "";
    for (int i = 0; i < n; i++)
      t1 = String.format("%07x", i);
    long e1 = System.nanoTime() - s1;

    long s2 = System.nanoTime();
    String t2 = "";
    for (int i = 0; i < n; i++)
      t2 = formatSimple("%07x", i);
    long e2 = System.nanoTime() - s2;

    long s3 = System.nanoTime();
    String t3 = "";
    for (int i = 0; i < n; i++)
      t3 = Integer.toHexString(0x10000000 | i).substring(1);
    long e3 = System.nanoTime() - s3;

    String line = String.format("Iteration: %d\n"
            + "String.format()       CPU Time(ns): %16d x  1.0\n"
            + "formatSimple()        CPU Time(ns): %16d x %4.1f\n"
            + "toHexString+substring CPU Time(ns): %16d x %4.1f\n\n",
        n, e1, e2, (1.0 * e1 / e2), e3, (1.0 * e1 / e3));
    Log.i("Benchmark", line);
    return line;
  }

  public @NonNull String formatSimple(@NonNull String format, Object... args) {
    final StringBuilder sb = new StringBuilder(format);
    int j = 0;
    for (int i = 0; i < sb.length(); ) {
      if (sb.charAt(i) == '%') {
        char code = sb.charAt(i + 1);

        // Decode any argument width request
        char prefixChar = '\0';
        int prefixLen = 0;
        int consume = 2;
        while ('0' <= code && code <= '9') {
          if (prefixChar == '\0') {
            prefixChar = (code == '0') ? '0' : ' ';
          }
          prefixLen *= 10;
          prefixLen += Character.digit(code, 10);
          consume += 1;
          code = sb.charAt(i + consume - 1);
        }

        final String repl;
        switch (code) {
          case 'b': {
            if (j == args.length) {
              throw new IllegalArgumentException("Too few arguments");
            }
            final Object arg = args[j++];
            if (arg instanceof Boolean) {
              repl = Boolean.toString((boolean) arg);
            } else {
              repl = Boolean.toString(arg != null);
            }
            break;
          }
          case 'c':
          case 'd':
          case 'f':
          case 's': {
            if (j == args.length) {
              throw new IllegalArgumentException("Too few arguments");
            }
            final Object arg = args[j++];
            repl = String.valueOf(arg);
            break;
          }
          case 'x': {
            if (j == args.length) {
              throw new IllegalArgumentException("Too few arguments");
            }
            final Object arg = args[j++];
            if (arg instanceof Integer) {
              repl = Integer.toHexString((int) arg);
            } else if (arg instanceof Long) {
              repl = Long.toHexString((long) arg);
            } else {
              throw new IllegalArgumentException(
                  "Unsupported hex type " + arg.getClass());
            }
            break;
          }
          case '%': {
            repl = "%";
            break;
          }
          default: {
            throw new IllegalArgumentException("Unsupported format code " + code);
          }
        }

        sb.replace(i, i + consume, repl);

        // Apply any argument width request
        final int prefixInsert = (prefixChar == '0' && repl.charAt(0) == '-') ? 1 : 0;
        for (int k = repl.length(); k < prefixLen; k++) {
          sb.insert(i + prefixInsert, prefixChar);
        }
        i += Math.max(repl.length(), prefixLen);
      } else {
        i++;
      }
    }
    if (j != args.length) {
      throw new IllegalArgumentException("Too many arguments");
    }
    return sb.toString();
  }
}