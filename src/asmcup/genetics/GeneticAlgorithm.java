package asmcup.genetics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import asmcup.evaluation.Evaluator;

public class GeneticAlgorithm {
	public final ArrayList<Gene> pinned;
	public final Random random;
	public final Evaluator evaluator;
	public Gene[] population;
	public int generation;
	public int mutationRate = 50;
	public int minMutationRate = 1;
	public int maxMutationRate = 100;
	public int mutationSize = 4;
	public int dnaLength = 256;

	// FIXME: Handle DNA length changes gracefully
	// TODO: Meaningful initial population?
	
	public GeneticAlgorithm(Evaluator evaluator) {
		this.evaluator = evaluator;

		random = new Random();
		population = new Gene[0];
		pinned = new ArrayList<>();
	}
	
	public Gene createGene(byte[] rom) {
		return new Gene(rom, generation, evaluator.score(rom));
	}

	public void initializePopulation(int populationSize) {
		population = new Gene[populationSize];
		
		for (int i=0; i < population.length; i++) {
			population[i] = randomGene();
		}
	}
	
	public void resizePopulation(int newSize) {
		Gene[] newPop = new Gene[newSize];
		
		for (int i=0; i < newSize; i++) {
			newPop[i] = randomGene();
		}
		
		population = newPop;
	}
	
	public void nextGeneration() {
		adjustMutationRate();
		
		int halfPoint = population.length / 2;
		int pin = pinned.size();
		
		for (int i=halfPoint; i < population.length; i++) {
			if (pin > 0) {
				pin--;
				population[i] = cross(pinned.get(pin), selectRandomGene());
			} else {
				population[i] = cross();
			}
		}
		
		Arrays.sort(population);
		generation++;
	}

	private byte randomByte() {
		return (byte)random.nextInt(256);
	}
	
	private Gene randomGene() {
		return createGene(randomDNA());
	}
	
	private byte[] randomDNA() {
		byte[] dna = new byte[256];
		
		for (int i = 0; i < dnaLength; i++) {
			dna[i] = randomByte();
		}
		
		return dna;
	}

	public Gene selectRandomGene() {
		int i = random.nextInt(population.length / 2);
		return population[i];
	}
	
	public Gene cross() {
		int a, b;
		
		do {
			a = random.nextInt(population.length / 2);
			b = random.nextInt(population.length / 2);
		} while (a == b);
		
		return cross(population[a], population[b]);
	}
	
	public Gene cross(Gene mom, Gene dad) {
		byte[] dna = mom.dna.clone();
		
		switch (random.nextInt(2)) {
		case 0:
			crossTwoPoint(mom, dad, dna);
			break;
		case 1:
		default:
			crossUniform(mom, dad, dna);
			break;	
		}
		
		if (random.nextInt(100) <= mutationRate) {
			mutate(dna);
		}
		
		return createGene(dna);
	}
	
	protected void crossTwoPoint(Gene mom, Gene dad, byte[] dna) {
		int dest, src, size;

		src = random.nextInt(dnaLength);
		dest = random.nextInt(dnaLength);
		size = 1 + random.nextInt(dnaLength);

		for (int i = 0; i < size; i++) {
			dna[(dest + i) % dnaLength] = dad.dna[(src + i) % dnaLength];
		}
	}
	
	protected void crossUniform(Gene mom, Gene dad, byte[] dna) {
		for (int i=0; i < dnaLength; i++) {
			if (random.nextBoolean()) {
				dna[i] = dad.dna[i];
			}
		}
	}
	
	protected void mutate(byte[] dna) {
		int dest = random.nextInt(dnaLength);
		int size = 1 + random.nextInt(mutationSize);
		int gap = 1 + random.nextInt(mutationSize);
		
		for (int i=0; i < size; i += gap) {
			dna[(dest + i) % dnaLength] = randomByte();
		}
	}

	public byte[] getBestDNA() {
		return population[0].dna.clone();
	}
	
	public float getBestScore() {
		return population[0].score;
	}
	
	public float getWorstScore() {
		return population[population.length / 2 - 1].score;
	}
	
	public void adjustMutationRate() {
		float p = getWorstScore() / getBestScore();
		// TODO: That's the laziest lerp I've ever seen...
		mutationRate = minMutationRate + (int)(p * maxMutationRate);
		mutationRate = Math.max(minMutationRate, mutationRate);
		mutationRate = Math.min(maxMutationRate, mutationRate);
	}
	
	public void pin() {
		pinned.add(population[0]);
	}
	
	public void clearPinned() {
		pinned.clear();
	}
	
	public void pin(byte[] dna) {
		pinned.add(createGene(dna));
	}
}