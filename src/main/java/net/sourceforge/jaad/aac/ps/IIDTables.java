package net.sourceforge.jaad.aac.ps;

/**
 * Created by IntelliJ IDEA.
 * User: stueken
 * Date: 25.09.21
 * Time: 12:20
 */
class IIDTables extends EnvTables {

    final int num_steps;

    final float[] sf;

    final float[][] cos_betas, sin_betas, cos_gammas, sin_gammas, sincos_alphas_b;

    IIDTables(int[][] f, int[][] t,
              int num_steps, float[] sf,
              float[][] cos_betas, float[][] sin_betas,
              float[][] cos_gammas, float[][] sin_gammas,
              float[][] sincos_alphas_b) {
        super(f, t);
        this.num_steps = num_steps;
        this.sf = sf;
        this.cos_betas = cos_betas;
        this.sin_betas = sin_betas;
        this.cos_gammas = cos_gammas;
        this.sin_gammas = sin_gammas;
        this.sincos_alphas_b = sincos_alphas_b;
    }
}
