/**
 * Copyright 2013-15 Simon Andrews
 *
 *    This file is part of SeqMonk.
 *
 *    SeqMonk is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    SeqMonk is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with SeqMonk; if not, write to the Free Software
 *    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package uk.ac.babraham.SeqMonk.Gradients;

import java.awt.Color;

import uk.ac.babraham.SeqMonk.Dialogs.CrashReporter;

public class WhiteCyanBlueRedColourGradient extends ColourGradient {

	protected Color[] makeColors() {

		Color [] colours = new Color [200];
		try {
			colours[0] = Color.decode("#FFFFFF");
			colours[1] = Color.decode("#FDFDFD");
			colours[2] = Color.decode("#FBFBFB");
			colours[3] = Color.decode("#F9F9F9");
			colours[4] = Color.decode("#F7F7F7");
			colours[5] = Color.decode("#F5F5F5");
			colours[6] = Color.decode("#F3F3F3");
			colours[7] = Color.decode("#F1F1F1");
			colours[8] = Color.decode("#EFEFEF");
			colours[9] = Color.decode("#EDEDED");
			colours[10] = Color.decode("#EBEBEB");
			colours[11] = Color.decode("#E9E9E9");
			colours[12] = Color.decode("#E7E7E7");
			colours[13] = Color.decode("#E5E5E5");
			colours[14] = Color.decode("#E3E3E3");
			colours[15] = Color.decode("#E1E1E1");
			colours[16] = Color.decode("#DFDFDF");
			colours[17] = Color.decode("#DDDDDD");
			colours[18] = Color.decode("#DBDBDB");
			colours[19] = Color.decode("#D9D9D9");
			colours[20] = Color.decode("#D7D7D7");
			colours[21] = Color.decode("#D5D5D5");
			colours[22] = Color.decode("#D3D3D3");
			colours[23] = Color.decode("#D1D1D1");
			colours[24] = Color.decode("#CFCFCF");
			colours[25] = Color.decode("#CECECE");
			colours[26] = Color.decode("#CCCCCC");
			colours[27] = Color.decode("#CACACA");
			colours[28] = Color.decode("#C8C8C8");
			colours[29] = Color.decode("#C6C6C6");
			colours[30] = Color.decode("#C4C4C4");
			colours[31] = Color.decode("#C2C2C2");
			colours[32] = Color.decode("#C0C0C0");
			colours[33] = Color.decode("#BEBEBE");
			colours[34] = Color.decode("#B9BFBF");
			colours[35] = Color.decode("#B3C1C1");
			colours[36] = Color.decode("#ADC3C3");
			colours[37] = Color.decode("#A8C5C5");
			colours[38] = Color.decode("#A2C7C7");
			colours[39] = Color.decode("#9CC9C9");
			colours[40] = Color.decode("#96CBCB");
			colours[41] = Color.decode("#91CDCD");
			colours[42] = Color.decode("#8BCFCF");
			colours[43] = Color.decode("#85D1D1");
			colours[44] = Color.decode("#7FD3D3");
			colours[45] = Color.decode("#7AD5D5");
			colours[46] = Color.decode("#74D7D7");
			colours[47] = Color.decode("#6ED9D9");
			colours[48] = Color.decode("#69DBDB");
			colours[49] = Color.decode("#63DDDD");
			colours[50] = Color.decode("#5DDEDE");
			colours[51] = Color.decode("#57E0E0");
			colours[52] = Color.decode("#52E2E2");
			colours[53] = Color.decode("#4CE4E4");
			colours[54] = Color.decode("#46E6E6");
			colours[55] = Color.decode("#40E8E8");
			colours[56] = Color.decode("#3BEAEA");
			colours[57] = Color.decode("#35ECEC");
			colours[58] = Color.decode("#2FEEEE");
			colours[59] = Color.decode("#2AF0F0");
			colours[60] = Color.decode("#24F2F2");
			colours[61] = Color.decode("#1EF4F4");
			colours[62] = Color.decode("#18F6F6");
			colours[63] = Color.decode("#13F8F8");
			colours[64] = Color.decode("#0DFAFA");
			colours[65] = Color.decode("#07FCFC");
			colours[66] = Color.decode("#01FEFE");
			colours[67] = Color.decode("#03FEFE");
			colours[68] = Color.decode("#08FDFD");
			colours[69] = Color.decode("#0DFBFC");
			colours[70] = Color.decode("#13FAFC");
			colours[71] = Color.decode("#18F9FB");
			colours[72] = Color.decode("#1DF8FA");
			colours[73] = Color.decode("#22F7F9");
			colours[74] = Color.decode("#27F5F9");
			colours[75] = Color.decode("#2DF4F8");
			colours[76] = Color.decode("#32F3F7");
			colours[77] = Color.decode("#37F2F6");
			colours[78] = Color.decode("#3CF1F6");
			colours[79] = Color.decode("#42F0F5");
			colours[80] = Color.decode("#47EEF4");
			colours[81] = Color.decode("#4CEDF3");
			colours[82] = Color.decode("#51ECF3");
			colours[83] = Color.decode("#56EBF2");
			colours[84] = Color.decode("#5CEAF1");
			colours[85] = Color.decode("#61E9F0");
			colours[86] = Color.decode("#66E7F0");
			colours[87] = Color.decode("#6BE6EF");
			colours[88] = Color.decode("#71E5EE");
			colours[89] = Color.decode("#76E4ED");
			colours[90] = Color.decode("#7BE3ED");
			colours[91] = Color.decode("#80E1EC");
			colours[92] = Color.decode("#85E0EB");
			colours[93] = Color.decode("#8BDFEA");
			colours[94] = Color.decode("#90DEEA");
			colours[95] = Color.decode("#95DDE9");
			colours[96] = Color.decode("#9ADCE8");
			colours[97] = Color.decode("#9FDAE7");
			colours[98] = Color.decode("#A5D9E7");
			colours[99] = Color.decode("#AAD8E6");
			colours[100] = Color.decode("#AAD4E6");
			colours[101] = Color.decode("#A5CEE7");
			colours[102] = Color.decode("#9FC7E7");
			colours[103] = Color.decode("#9AC1E8");
			colours[104] = Color.decode("#95BAE9");
			colours[105] = Color.decode("#90B4EA");
			colours[106] = Color.decode("#8BADEA");
			colours[107] = Color.decode("#85A7EB");
			colours[108] = Color.decode("#80A0EC");
			colours[109] = Color.decode("#7B9AED");
			colours[110] = Color.decode("#7693ED");
			colours[111] = Color.decode("#718DEE");
			colours[112] = Color.decode("#6B86EF");
			colours[113] = Color.decode("#6680F0");
			colours[114] = Color.decode("#6179F0");
			colours[115] = Color.decode("#5C73F1");
			colours[116] = Color.decode("#566CF2");
			colours[117] = Color.decode("#5166F3");
			colours[118] = Color.decode("#4C5FF3");
			colours[119] = Color.decode("#4759F4");
			colours[120] = Color.decode("#4252F5");
			colours[121] = Color.decode("#3C4BF6");
			colours[122] = Color.decode("#3745F6");
			colours[123] = Color.decode("#323EF7");
			colours[124] = Color.decode("#2D38F8");
			colours[125] = Color.decode("#2731F9");
			colours[126] = Color.decode("#222BF9");
			colours[127] = Color.decode("#1D24FA");
			colours[128] = Color.decode("#181EFB");
			colours[129] = Color.decode("#1317FC");
			colours[130] = Color.decode("#0D11FC");
			colours[131] = Color.decode("#080AFD");
			colours[132] = Color.decode("#0304FE");
			colours[133] = Color.decode("#0201FE");
			colours[134] = Color.decode("#0A07FC");
			colours[135] = Color.decode("#110DFB");
			colours[136] = Color.decode("#1913F9");
			colours[137] = Color.decode("#2119F8");
			colours[138] = Color.decode("#291EF6");
			colours[139] = Color.decode("#3024F5");
			colours[140] = Color.decode("#382AF3");
			colours[141] = Color.decode("#4030F1");
			colours[142] = Color.decode("#4736F0");
			colours[143] = Color.decode("#4F3BEE");
			colours[144] = Color.decode("#5741ED");
			colours[145] = Color.decode("#5E47EB");
			colours[146] = Color.decode("#664DEA");
			colours[147] = Color.decode("#6E52E8");
			colours[148] = Color.decode("#7558E6");
			colours[149] = Color.decode("#7D5EE5");
			colours[150] = Color.decode("#8564E3");
			colours[151] = Color.decode("#8C6AE2");
			colours[152] = Color.decode("#946FE0");
			colours[153] = Color.decode("#9C75DF");
			colours[154] = Color.decode("#A47BDD");
			colours[155] = Color.decode("#AB81DB");
			colours[156] = Color.decode("#B387DA");
			colours[157] = Color.decode("#BB8CD8");
			colours[158] = Color.decode("#C292D7");
			colours[159] = Color.decode("#CA98D5");
			colours[160] = Color.decode("#D29ED4");
			colours[161] = Color.decode("#D9A4D2");
			colours[162] = Color.decode("#E1A9D1");
			colours[163] = Color.decode("#E9AFCF");
			colours[164] = Color.decode("#F0B5CD");
			colours[165] = Color.decode("#F8BBCC");
			colours[166] = Color.decode("#FFBFC9");
			colours[167] = Color.decode("#FFB9C3");
			colours[168] = Color.decode("#FFB3BD");
			colours[169] = Color.decode("#FFADB7");
			colours[170] = Color.decode("#FFA7B1");
			colours[171] = Color.decode("#FFA2AB");
			colours[172] = Color.decode("#FF9CA5");
			colours[173] = Color.decode("#FF969F");
			colours[174] = Color.decode("#FF9099");
			colours[175] = Color.decode("#FF8A92");
			colours[176] = Color.decode("#FF858C");
			colours[177] = Color.decode("#FF7F86");
			colours[178] = Color.decode("#FF7980");
			colours[179] = Color.decode("#FF737A");
			colours[180] = Color.decode("#FF6D74");
			colours[181] = Color.decode("#FF686E");
			colours[182] = Color.decode("#FF6268");
			colours[183] = Color.decode("#FF5C61");
			colours[184] = Color.decode("#FF565B");
			colours[185] = Color.decode("#FF5155");
			colours[186] = Color.decode("#FF4B4F");
			colours[187] = Color.decode("#FF4549");
			colours[188] = Color.decode("#FF3F43");
			colours[189] = Color.decode("#FF393D");
			colours[190] = Color.decode("#FF3437");
			colours[191] = Color.decode("#FF2E30");
			colours[192] = Color.decode("#FF282A");
			colours[193] = Color.decode("#FF2224");
			colours[194] = Color.decode("#FF1C1E");
			colours[195] = Color.decode("#FF1718");
			colours[196] = Color.decode("#FF1112");
			colours[197] = Color.decode("#FF0B0C");
			colours[198] = Color.decode("#FF0506");
			colours[199] = Color.decode("#FF0000");
		}
		catch(NumberFormatException nfe) {
			new CrashReporter(nfe);
			return null;
		}
		return colours;
	}

	public String name() {
		return "White>Cyan>Blue>Red Gradient";
	}

}
