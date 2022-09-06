package unicam.pi.mqespol.view;


import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.format.Formatter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttException;

import java.util.List;
import java.util.Objects;

import unicam.pi.mqespol.databinding.FragmentListDeviceBinding;
import unicam.pi.mqespol.model.Device;
import unicam.pi.mqespol.mqtt.MqttConnection;
import unicam.pi.mqespol.mqtt.mqttService;
import unicam.pi.mqespol.util.WifiFuctions;
import unicam.pi.mqespol.util.util;
import unicam.pi.mqespol.view.adapters.DeviceAdapter;
import unicam.pi.mqespol.view.adapters.SsidAdapter;
import unicam.pi.mqespol.viewModel.DeviceViewModel;


public class FragmentListDevice extends Fragment {

    private DeviceViewModel deviceViewModel;
    private FragmentListDeviceBinding binding;
    private DeviceAdapter deviceAdapter;
    MqttAndroidClient mqttAndroidClient;
    SsidAdapter ssidAdapter;

    Intent serviceMQTT;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        deviceViewModel = new ViewModelProvider(this).get(DeviceViewModel.class);
        Log.d("TAG","LIST DEVICE FRAGMENT");
        if(!WifiFuctions.isHostPotOn){
            WifiFuctions.setWifiOff();
            deviceViewModel.setHotspotOn(getActivity());  //ACTIVAR EL HOSTPOT OnHostPot Wifi
            initApp();
        }
        WifiManager wm = (WifiManager)this.getContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo connectionInfo = wm.getConnectionInfo();
        int ipAddress = connectionInfo.getIpAddress();
        String ipString = Formatter.formatIpAddress(ipAddress);
        Log.e("TAG","IP WIFI MANAGER "+ ipString);
        mqttAndroidClient = new MqttAndroidClient(this.getContext(),"tcp:"+ipString+":1883", util.CLIENT_ID);
        IMqttToken token = null;
        try {
            token = mqttAndroidClient.connect();
        } catch (MqttException e) {
            e.printStackTrace();
        }
        token.setActionCallback(new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
                Toast.makeText(getContext(),"connected!!",Toast.LENGTH_LONG).show();
                Log.e("TAG", "CONEXION EXITOsa");
            }

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                Toast.makeText(getContext(),"Conexion Fallida",Toast.LENGTH_LONG).show();

            }
        });
        deviceViewModel.getAllDevices().observe(getViewLifecycleOwner(), new Observer<List<Device>>() {
            @Override
            public void onChanged(List<Device> devices) {
                deviceAdapter.submitList(devices);
                Log.e("TAG",devices.toString());
                if (devices != null) {
                    connecClient(devices);    //INICIALIZA LA CONEXION  CON EL BROKER LOCAL
                }
            }
        });
//        deviceViewModel.getMensaje().observe(getViewLifecycleOwner(), new Observer<String>() {
//            @Override
//            public void onChanged(String s) {
//                if(s!=null){
//                    Log.d("TAG","TOAST LOGEADO");
//                     toast("Mensaje Recibido del Broker: "+s);
//                }
//            }
//        });

        initRecyclerView();
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                    deviceViewModel.delete(deviceAdapter.getDeviceAt(viewHolder.getAdapterPosition()));
                    toast("Device Deleted");
            }
        }).attachToRecyclerView(binding.recyclerView);
    }

    void initApp(){
        serviceMQTT  = new Intent(getContext(), mqttService.class);
        this.getContext().startForegroundService(serviceMQTT);
    }

    public void initRecyclerView(){
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerView.setHasFixedSize(true);
        deviceAdapter = new DeviceAdapter();
        binding.recyclerView.setAdapter(deviceAdapter);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentListDeviceBinding.inflate(getLayoutInflater());
        return binding.getRoot();
    }

    @Override
    public void onPause() {
        super.onPause();
        this.getContext().stopService(serviceMQTT);
    }
    public void connecClient(List<Device> devices) {
        try {
            IMqttToken token = mqttAndroidClient.connect();
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    deviceViewModel.loadSubcription(mqttAndroidClient,devices);   // SE SUBSCRIBE A LA LISTA DE DEVICES CONECTADOS.
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Toast.makeText(getContext(), "Connection Failed", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
    public void toast(String msg){
        Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
    }
}