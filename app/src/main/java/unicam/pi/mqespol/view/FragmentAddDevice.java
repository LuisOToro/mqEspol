package unicam.pi.mqespol.view;


import android.net.wifi.ScanResult;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import org.slf4j.helpers.Util;

import java.util.List;

import unicam.pi.mqespol.R;
import unicam.pi.mqespol.databinding.FragmentAddDeviceBinding;
import unicam.pi.mqespol.util.util;
import unicam.pi.mqespol.view.adapters.SsidAdapter;
import unicam.pi.mqespol.viewModel.DeviceViewModel;

public class FragmentAddDevice extends Fragment  {

    private SsidAdapter ssidAdapter;
    private FragmentAddDeviceBinding binding;
    private DeviceViewModel deviceViewModel;
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Toast.makeText(getContext(), "WiFi ON!", Toast.LENGTH_SHORT).show();
        deviceViewModel = new ViewModelProvider(this).get(DeviceViewModel.class);
        initRecyclerView();

        binding.btnScan.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (MainActivity.wifiManager.isWifiEnabled()) {
                   toast("Scanning Networks...");
                   deviceViewModel.scanWifi(MainActivity.wifiManager, MainActivity.wifiReciever, requireActivity());
               }
                else {
                    toast("WiFi is OFF.. Turn it ON for Scanning");
                }
            }
            }
        );
        deviceViewModel.get().observe(getViewLifecycleOwner(), new Observer<List<ScanResult>>() {
            @Override
            public void onChanged(List<ScanResult> scanResults) {
                ssidAdapter.setSSID(scanResults);
            }
        });

        binding.btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (util.isValid(binding.lblNameDevice.getText().toString(), ssidAdapter.getItemSelected())) {
                    if (deviceViewModel.addDevice(ssidAdapter.getItemSelected(), binding.lblNameDevice.getText().toString())) {
                        toast("Device Connected");
                        FragmentListDevice listFragment = new FragmentListDevice();
                        getActivity().getSupportFragmentManager().beginTransaction()
                                .replace(R.id.fragmentContainer, listFragment)
                                .commit();                    } else {
                        toast("Topic Device Wrong Format, Not Added");
                    }
                } else {
                    toast("Please Type a Name and Select a Network");
                }
            }
        });

    }


    public void initRecyclerView(){
        binding.recyclerViewSsid.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerViewSsid.setHasFixedSize(true);
        ssidAdapter = new SsidAdapter(deviceViewModel.getListWifi());
        binding.recyclerViewSsid.setAdapter(ssidAdapter);
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding  = FragmentAddDeviceBinding.inflate(getLayoutInflater());
        return binding.getRoot();
    }

    public void toast(String msg){
        Toast.makeText(getContext(), msg,Toast.LENGTH_SHORT).show();
    }
}