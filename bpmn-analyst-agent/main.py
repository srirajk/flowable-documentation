import os
import subprocess
import json

# --- Configuration ---
AGENT_CAPABILITIES_PATH = "bpmn-analyst-agent/agent_capabilities.md"
GENERATED_BPMN_PATH = "bpmn-analyst-agent/generated_workflow.bpmn20.xml"
TEST_SUMMARY_PATH = "bpmn-analyst-agent/test_summary.md"
API_BASE_URL = "http://localhost:8080"

# --- Tool Definitions ---

def write_file(file_path: str, content: str) -> str:
    """Writes content to a specified file."""
    try:
        os.makedirs(os.path.dirname(file_path), exist_ok=True)
        with open(file_path, 'w') as f:
            f.write(content)
        return f"Successfully wrote to {file_path}"
    except Exception as e:
        return f"Error writing to file: {e}"

def run_shell_command(command: str) -> str:
    """Executes a shell command and returns its output."""
    try:
        print(f"Executing command: {command}")
        result = subprocess.run(command, shell=True, capture_output=True, text=True, check=True)
        return result.stdout
    except subprocess.CalledProcessError as e:
        return f"Command failed with error:\nSTDOUT: {e.stdout}\nSTDERR: {e.stderr}"
    except Exception as e:
        return f"An unexpected error occurred: {e}"

# --- Agent Core Logic ---

class BpmnBuilderAgent:
    def __init__(self):
        self.capabilities = self._load_capabilities()
        # In a real scenario, this would be a proper LLM client (e.g., from OpenAI, Anthropic, Google)
        # For this example, we'll simulate the LLM's response for generating BPMN.
        self.llm_client = self._get_llm_client() 

    def _load_capabilities(self) -> str:
        """Loads the agent's capabilities from the markdown file."""
        try:
            with open(AGENT_CAPABILITIES_PATH, 'r') as f:
                return f.read()
        except FileNotFoundError:
            return "Error: Agent capabilities file not found."

    def _get_llm_client(self):
        """
        This is a placeholder for a real LLM client.
        It simulates the LLM's ability to generate BPMN based on a prompt.
        """
        def generate_bpmn_from_prompt(user_prompt: str, system_prompt: str) -> str:
            # In a real implementation, you would send the user_prompt and system_prompt
            # to the LLM API and return the response.
            # Here, we'll use a simple, hardcoded example for demonstration.
            print("\n--- Simulating LLM Call to Generate BPMN ---")
            print(f"System Prompt contains: {len(system_prompt)} characters of capabilities.")
            print(f"User Prompt: {user_prompt}")
            
            # This is a hardcoded example response for a specific prompt.
            # A real LLM would generate this dynamically.
            if "two-step approval" in user_prompt.lower():
                return """<?xml version=\"1.0\" encoding=\"UTF-8\"?>
<definitions xmlns=\"http://www.omg.org/spec/BPMN/20100524/MODEL\"
             xmlns:flowable=\"http://flowable.org/bpmn\"
             targetNamespace=\"http://www.flowable.org/processdef\">
    <process id=\"twoStepApproval\" name=\"Two-Step Approval Process\">
        <startEvent id=\"start\"/>
        <sequenceFlow sourceRef=\"start\" targetRef=\"managerApproval\"/>
        <userTask id=\"managerApproval\" name=\"Manager Approval\" flowable:candidateGroups=\"managers\"/>
        <sequenceFlow sourceRef=\"managerApproval\" targetRef=\"financeApproval\"/>
        <userTask id=\"financeApproval\" name=\"Finance Approval\" flowable:candidateGroups=\"finance\"/>
        <sequenceFlow sourceRef=\"financeApproval\" targetRef=\"end\"/>
        <endEvent id=\"end\"/>
    </process>
</definitions>"""
            else:
                return "Error: Could not generate BPMN for this request. Only 'two-step approval' is supported in this simulation."
        
        return generate_bpmn_from_prompt

    def execute_task(self, user_prompt: str):
        """Main execution loop for the agent."""
        print("--- Agent Task Started ---")
        
        # 1. Generate BPMN
        print("\nStep 1: Generating BPMN...")
        system_prompt = f"You are a BPMN Builder Agent. Follow these rules:\n{self.capabilities}"
        bpmn_content = self.llm_client(user_prompt, system_prompt)
        if "Error:" in bpmn_content:
            print(bpmn_content)
            return

        # 2. Save BPMN to file
        print("\nStep 2: Saving BPMN file...")
        result = write_file(GENERATED_BPMN_PATH, bpmn_content)
        print(result)

        # 3. Deploy BPMN
        print("\nStep 3: Deploying BPMN...")
        deploy_command = f"curl -X POST -F 'file=@{GENERATED_BPMN_PATH}' {API_BASE_URL}/deploy"
        deploy_result = run_shell_command(deploy_command)
        print(f"Deployment result: {deploy_result}")
        
        try:
            process_definition_id = json.loads(deploy_result).get("processDefinitionId")
            if not process_definition_id:
                print("Error: Could not get processDefinitionId from deployment.")
                return
        except (json.JSONDecodeError, AttributeError):
            print(f"Error: Failed to parse deployment response: {deploy_result}")
            return
            
        print(f"Successfully deployed. Process Definition ID: {process_definition_id}")

        # 4. Test the process
        print("\nStep 4: Testing the process...")
        # For now, we will just start the process. A full test loop would be more complex.
        start_command = f"curl -X POST -H 'Content-Type: application/json' -d '{{\"processDefinitionId\": \"{process_definition_id}\"}}' {API_BASE_URL}/start"
        start_result = run_shell_command(start_command)
        print(f"Start process result: {start_result}")
        
        try:
            process_instance_id = json.loads(start_result).get("processInstanceId")
            if not process_instance_id:
                print("Error: Could not get processInstanceId from start command.")
                return
        except (json.JSONDecodeError, AttributeError):
            print(f"Error: Failed to parse start response: {start_result}")
            return
            
        print(f"Successfully started instance. Process Instance ID: {process_instance_id}")

        # 5. Generate Report (Simplified)
        print("\nStep 5: Generating report...")
        report_content = f"""# Workflow Test Summary

## User Request
> {user_prompt}

## Deployment
- **Status:** Success
- **Process Definition ID:** {process_definition_id}

## Test Execution
- **Status:** Started
- **Process Instance ID:** {process_instance_id}

**Note:** This is a simplified test run. The agent started the process but did not complete the tasks.
"""
        report_result = write_file(TEST_SUMMARY_PATH, report_content)
        print(report_result)
        
        print("\n--- Agent Task Finished ---")


if __name__ == "__main__":
    agent = BpmnBuilderAgent()
    
    # This is a sample prompt. In a real application, this would come from user input.
    prompt = "I need a simple two-step approval workflow for a document, first by a manager, then by the finance team."
    
    agent.execute_task(prompt)
