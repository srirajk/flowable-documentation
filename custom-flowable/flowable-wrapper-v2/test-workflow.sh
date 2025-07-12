#!/bin/bash

# Purchase Order Approval Workflow Test Script
# This script helps test the workflow API endpoints

API_BASE="http://localhost:8090/api"

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${BLUE}Purchase Order Approval Workflow Test${NC}"
echo "======================================"

# Function to check if API is available
check_api() {
    echo -n "Checking API health... "
    if curl -s "$API_BASE/../actuator/health" | grep -q "UP"; then
        echo -e "${GREEN}OK${NC}"
    else
        echo -e "${RED}FAILED${NC}"
        echo "Please ensure the application is running on port 8090"
        exit 1
    fi
}

# Function to register workflow
register_workflow() {
    echo -e "\n${BLUE}1. Registering Workflow Metadata${NC}"
    
    RESPONSE=$(curl -s -X POST "$API_BASE/workflow-metadata/register" \
        -H "Content-Type: application/json" \
        -d '{
            "processDefinitionKey": "purchaseOrderApproval",
            "processName": "Purchase Order Approval Process",
            "description": "Multi-level purchase order approval with amount-based routing",
            "candidateGroupMappings": {
                "managers": "manager-queue",
                "directors": "director-queue",
                "finance": "finance-queue",
                "procurement": "procurement-queue"
            }
        }')
    
    if echo "$RESPONSE" | grep -q "processDefinitionKey"; then
        echo -e "${GREEN}✓ Workflow registered successfully${NC}"
        echo "$RESPONSE" | jq .
    else
        echo -e "${RED}✗ Failed to register workflow${NC}"
        echo "$RESPONSE"
    fi
}

# Function to deploy BPMN
deploy_bpmn() {
    echo -e "\n${BLUE}2. Deploying BPMN Workflow${NC}"
    
    # Check if BPMN file exists
    if [ ! -f "purchase-order-approval.bpmn20.xml" ]; then
        echo -e "${RED}✗ BPMN file not found: purchase-order-approval.bpmn20.xml${NC}"
        return 1
    fi
    
    # Read and escape BPMN content
    BPMN_CONTENT=$(cat purchase-order-approval.bpmn20.xml | jq -Rs .)
    
    RESPONSE=$(curl -s -X POST "$API_BASE/workflow-metadata/deploy" \
        -H "Content-Type: application/json" \
        -d "{
            \"processDefinitionKey\": \"purchaseOrderApproval\",
            \"bpmnXml\": $BPMN_CONTENT,
            \"deploymentName\": \"Purchase Order Approval v1.0\"
        }")
    
    if echo "$RESPONSE" | grep -q "deployed.*true"; then
        echo -e "${GREEN}✓ BPMN deployed successfully${NC}"
        echo "$RESPONSE" | jq '{deploymentId, deployed, taskQueueMappings}'
    else
        echo -e "${RED}✗ Failed to deploy BPMN${NC}"
        echo "$RESPONSE"
    fi
}

# Function to start a process
start_process() {
    local AMOUNT=$1
    local ORDER_ID=$2
    local DESCRIPTION=$3
    
    echo -e "\n${BLUE}Starting Process: $ORDER_ID (Amount: \$$AMOUNT)${NC}"
    
    RESPONSE=$(curl -s -X POST "$API_BASE/process-instances/start" \
        -H "Content-Type: application/json" \
        -d "{
            \"processDefinitionKey\": \"purchaseOrderApproval\",
            \"businessKey\": \"$ORDER_ID\",
            \"variables\": {
                \"orderId\": \"$ORDER_ID\",
                \"requester\": \"test.user@company.com\",
                \"department\": \"Testing\",
                \"amount\": $AMOUNT,
                \"description\": \"$DESCRIPTION\",
                \"urgency\": \"normal\"
            }
        }")
    
    if echo "$RESPONSE" | grep -q "processInstanceId"; then
        echo -e "${GREEN}✓ Process started successfully${NC}"
        PROCESS_ID=$(echo "$RESPONSE" | jq -r .processInstanceId)
        echo "Process Instance ID: $PROCESS_ID"
    else
        echo -e "${RED}✗ Failed to start process${NC}"
        echo "$RESPONSE"
    fi
}

# Function to check queue
check_queue() {
    local QUEUE=$1
    echo -e "\n${BLUE}Checking $QUEUE${NC}"
    
    RESPONSE=$(curl -s "$API_BASE/tasks/queue/$QUEUE")
    COUNT=$(echo "$RESPONSE" | jq length)
    
    if [ "$COUNT" -gt 0 ]; then
        echo -e "${GREEN}Found $COUNT task(s) in $QUEUE${NC}"
        echo "$RESPONSE" | jq '.[] | {taskId, taskName, businessKey, status}'
    else
        echo "No tasks in $QUEUE"
    fi
}

# Function to show all queues
show_all_queues() {
    echo -e "\n${BLUE}Queue Status Summary${NC}"
    echo "===================="
    
    for queue in manager-queue director-queue finance-queue procurement-queue; do
        COUNT=$(curl -s "$API_BASE/tasks/queue/$queue" | jq length)
        echo "$queue: $COUNT task(s)"
    done
}

# Main menu
show_menu() {
    echo -e "\n${BLUE}Select an option:${NC}"
    echo "1. Setup workflow (register + deploy)"
    echo "2. Start low-value order (<\$5000)"
    echo "3. Start high-value order (≥\$5000)"
    echo "4. Check all queues"
    echo "5. Check specific queue"
    echo "6. Exit"
    echo -n "Choice: "
}

# Main execution
check_api

while true; do
    show_menu
    read choice
    
    case $choice in
        1)
            register_workflow
            sleep 1
            deploy_bpmn
            ;;
        2)
            ORDER_ID="PO-TEST-$(date +%s)"
            start_process 3500 "$ORDER_ID" "Test low-value order"
            ;;
        3)
            ORDER_ID="PO-TEST-$(date +%s)"
            start_process 15000 "$ORDER_ID" "Test high-value order"
            ;;
        4)
            show_all_queues
            ;;
        5)
            echo -n "Enter queue name (e.g., manager-queue): "
            read queue_name
            check_queue "$queue_name"
            ;;
        6)
            echo "Exiting..."
            exit 0
            ;;
        *)
            echo -e "${RED}Invalid choice${NC}"
            ;;
    esac
done