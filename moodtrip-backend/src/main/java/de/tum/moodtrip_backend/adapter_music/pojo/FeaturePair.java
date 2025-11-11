package de.tum.moodtrip_backend.adapter_music.pojo;


import java.util.ArrayList;

public class FeaturePair {
    private final float energy;
    private final float valence;


    public FeaturePair(float energy, float valence) {
        this.energy = energy;
        this.valence = valence;

    }



    public float getEnergy() {
        return energy;
    }

    public float getValence() {
        return valence;
    }


    @Override
    public String toString() {
        return String.format("FeaturePair{energy=%.2f, valence=%.2f}", energy, valence);
    }
}
