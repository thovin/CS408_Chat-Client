package edu.jsu.mcis.cs408.chatclient;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;
import android.view.View;

import org.json.JSONObject;

import edu.jsu.mcis.cs408.chatclient.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private WebServiceViewModel model;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        model = new ViewModelProvider(this).get(WebServiceViewModel.class);

        final Observer<String> outputObserver = new Observer<String>() {
            @Override
            public void onChanged(String s) {
                if (s != null) {
                    setOutputText(s);
                }
            }
        };

        model.getOutput().observe(this, outputObserver);

        binding.postButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String input = binding.input.getText().toString();
                if (!input.equals("")) {
                    model.sendMessage(input);
                } else {
                    model.generateOutput();
                }
            }
        });

        binding.clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                model.clearChat();
            }
        });

        model.generateOutput();
    }

    private void setOutputText(String output) {
        binding.Output.setText(output);
    }
}