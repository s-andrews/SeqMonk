extern crate exec;

use std::env;
use std::process;

fn main() {
    let argv: Vec<String> = env::args().skip(1).collect();

    let err = exec::Command::new("seqmonk").args(&argv).exec();

    println!("Error launching seqmonk: {}",err);
    process::exit(1);
}
