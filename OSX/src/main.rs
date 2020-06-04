extern crate exec;

use std::env;
use std::process;
use std::path::PathBuf;

fn main() {
    let argv: Vec<String> = env::args().skip(1).collect();
    
    let mut dir = match env::current_exe() {
    	Ok(f) => f,
    	Err(e) => PathBuf::from(".")
    };
    
    dir.pop();
    dir.push("seqmonk");

    let err = exec::Command::new(dir.into_os_string()).args(&argv).exec();

    println!("Error launching seqmonk: {}",err);
    process::exit(1);
    
}
