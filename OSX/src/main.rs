extern crate exec;

use std::env;
use std::process;

fn main() {
    let argv: Vec<String> = env::args().skip(1).collect();
    
    let mut dir = env::current_exe()?;
    dir.pop();
    dir.push("seqmonk");

    let err = exec::Command::new(dir).args(&argv).exec();

    println!("Error launching seqmonk: {}",err);
    process::exit(1);
}
